# 手动 APK 构建脚本：aapt2 -> javac -> d8 -> 打包 dex -> zipalign -> apksigner
$ErrorActionPreference = "Stop"
$proj  = "C:\Users\Administrator\.openclaw-autoclaw\workspace\luopan-android"
$bt    = "C:\android-build\android-14"
$plat  = "C:\android-build\android-34\android.jar"
$build = "$proj\build"

Remove-Item -Recurse -Force $build -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force "$build" | Out-Null

Write-Host "== 1. aapt2 compile =="
& "$bt\aapt2.exe" compile --dir "$proj\res" -o "$build\res.zip"
if ($LASTEXITCODE -ne 0) { throw "aapt2 compile failed" }

Write-Host "== 2. aapt2 link =="
& "$bt\aapt2.exe" link -o "$build\base.apk" -I $plat `
    --manifest "$proj\AndroidManifest.xml" `
    --java "$build\gen" `
    --min-sdk-version 21 --target-sdk-version 34 `
    --auto-add-overlay `
    "$build\res.zip"
if ($LASTEXITCODE -ne 0) { throw "aapt2 link failed" }

Write-Host "== 3. javac =="
$srcs = @(Get-ChildItem "$proj\java\com\fengshui\luopan\*.java" | ForEach-Object { $_.FullName })
$srcs += "$build\gen\com\fengshui\luopan\R.java"
& javac -source 1.8 -target 1.8 -Xlint:-options -encoding UTF-8 -classpath $plat -d "$build\classes" @srcs
if ($LASTEXITCODE -ne 0) { throw "javac failed" }

Write-Host "== 4. d8 dex =="
& "$bt\d8.bat" --release --lib $plat --min-api 21 --output "$build" `
    (Get-ChildItem "$build\classes\com\fengshui\luopan\*.class" | ForEach-Object { $_.FullName })
if ($LASTEXITCODE -ne 0) { throw "d8 failed" }
if (-not (Test-Path "$build\classes.dex")) { throw "classes.dex missing" }

Write-Host "== 5. 打包 classes.dex 进 apk =="
python -c "import zipfile; zipfile.ZipFile(r'$build\base.apk','a',zipfile.ZIP_DEFLATED).write(r'$build\classes.dex','classes.dex'); print('dex packed')"

Write-Host "== 6. zipalign =="
& "$bt\zipalign.exe" -f 4 "$build\base.apk" "$build\aligned.apk"
if ($LASTEXITCODE -ne 0) { throw "zipalign failed" }

Write-Host "== 7. 生成签名密钥（如无） =="
if (-not (Test-Path "$build\release.keystore")) {
    & keytool -genkeypair -keystore "$build\release.keystore" -alias luopan `
        -keyalg RSA -keysize 2048 -validity 10950 `
        -storepass luopan2026 -keypass luopan2026 `
        -dname "CN=Fengshui Luopan,O=OpenClaw,C=CN"
}

Write-Host "== 8. apksigner 签名 =="
& "$bt\apksigner.bat" sign --ks "$build\release.keystore" --ks-key-alias luopan `
    --ks-pass pass:luopan2026 --key-pass pass:luopan2026 `
    --out "$build\luopan-v1.38.apk" "$build\aligned.apk"
if ($LASTEXITCODE -ne 0) { throw "apksigner failed" }

Write-Host "== 9. 校验 =="
& "$bt\apksigner.bat" verify --print-certs "$build\luopan-v1.38.apk"
Write-Host "BUILD-OK: $build\luopan-v1.38.apk"
