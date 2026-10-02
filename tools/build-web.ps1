#Requires -Version 5.1
<#
  Virgo —— 离线网页资源生成脚本

  把 web-src 里的纯前端资源打包成 WebView 可直接加载的静态站点：
      web-src  ->  app/src/main/assets/www

  设计要点：
    * 保留 csTimer 全部离线功能（计时 / 打乱 / 统计 / 会话 / 工具 / 求解器 / 虚拟魔方）；
    * 仅剔除两个纯在线模块：tools/onlinecomp.js、tools/battle.js；
    * 多语言只打包简体中文（lang/zh-cn.js），因此语言列表只保留 zh-cn；
    * 不引入任何外部资源，产物完全自包含。

  用法： powershell -ExecutionPolicy Bypass -File tools\build-web.ps1
#>

$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $PSScriptRoot
$srcRoot = Join-Path $repoRoot 'web-src'
$outRoot = Join-Path $repoRoot 'app\src\main\assets\www'

if (-not (Test-Path $srcRoot)) { throw "找不到网页资源源目录：$srcRoot" }

function Write-Utf8NoBom {
    param([string]$Path, [string]$Text)
    $dir = Split-Path -Parent $Path
    if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Force -Path $dir | Out-Null }
    [System.IO.File]::WriteAllText($Path, $Text, (New-Object System.Text.UTF8Encoding($false)))
}

function Read-Utf8 {
    param([string]$Path)
    return [System.IO.File]::ReadAllText($Path, [System.Text.Encoding]::UTF8)
}

function Normalize-Newlines {
    param([string]$Text)
    return (($Text -split "\r?\n") -join "`n")
}

# ---------------------------------------------------------------- 1. 清理输出
Write-Host "[1/5] 清理输出目录：$outRoot"
if (Test-Path $outRoot) { Remove-Item -Force -Recurse $outRoot }
New-Item -ItemType Directory -Force -Path $outRoot | Out-Null

# ------------------------------------------------------------ 2. 拷贝静态资源
Write-Host '[2/5] 拷贝 js / css / lang / 图标'
Copy-Item -Recurse -Force (Join-Path $srcRoot 'js') (Join-Path $outRoot 'js')
New-Item -ItemType Directory -Force -Path (Join-Path $outRoot 'css') | Out-Null
Copy-Item -Force (Join-Path $srcRoot 'css\style.css') (Join-Path $outRoot 'css\style.css')
New-Item -ItemType Directory -Force -Path (Join-Path $outRoot 'lang') | Out-Null
Copy-Item -Force (Join-Path $srcRoot 'lang\zh-cn.js') (Join-Path $outRoot 'lang\zh-cn.js')
Copy-Item -Force (Join-Path $srcRoot 'sw.js') (Join-Path $outRoot 'sw.js')
Copy-Item -Force (Join-Path $srcRoot 'cstimer512x512.png') (Join-Path $outRoot 'cstimer512x512.png')
foreach ($extra in @('LICENSE', 'COPYING')) {
    $p = Join-Path $srcRoot $extra
    if (Test-Path $p) { Copy-Item -Force $p (Join-Path $outRoot $extra) }
}

# ------------------------------------------------------------ 3. 剔除在线模块
Write-Host '[3/5] 剔除纯在线模块'
foreach ($rel in @('js\tools\onlinecomp.js', 'js\tools\battle.js')) {
    $p = Join-Path $outRoot $rel
    if (Test-Path $p) {
        Remove-Item -Force $p
        Write-Host "      已删除 $rel"
    }
}

# ------------------------------------------------------------ 4. 解析版本号
Write-Host '[4/5] 解析 csTimer 版本号'
$langDet = Read-Utf8 (Join-Path $srcRoot 'lang\langDet.php')
$version = [regex]::Match($langDet, '\$version\s*=\s*"([^"]*)"').Groups[1].Value
if (-not $version) { throw '无法从 lang/langDet.php 解析出版本号' }
Write-Host "      csTimer 版本：$version"

# ------------------------------------------------------------ 5. 生成 index.html
Write-Host '[5/5] 生成 index.html'

$indexPhp = Normalize-Newlines (Read-Utf8 (Join-Path $srcRoot 'index.php'))

# 5.1 脚本列表：照抄 index.php 中的 <script src>，顺序不变，只跳过两个在线模块
$skipScripts = @('js/tools/onlinecomp.js', 'js/tools/battle.js')
$scriptLines = New-Object System.Collections.Generic.List[string]
foreach ($m in [regex]::Matches($indexPhp, '<script[^>]*src="([^"]+)"[^>]*>\s*</script>')) {
    $rel = $m.Groups[1].Value
    if ($skipScripts -contains $rel) { continue }
    $scriptLines.Add("  <script type=`"text/javascript`" src=`"$rel`"></script>")
}
if ($scriptLines.Count -lt 50) { throw "index.php 中解析到的脚本数量异常：$($scriptLines.Count)" }

# 5.2 body DOM：取 index.php 的 <body>...</body>，去掉 php 片段并把 logo 改成 Virgo
$bodyMatch = [regex]::Match($indexPhp, '(?s)<body>(.*)</body>')
if (-not $bodyMatch.Success) { throw 'index.php 中找不到 body 内容' }
$body = $bodyMatch.Groups[1].Value
$body = $body.Replace('<span>csTimer</span>', '<span>Virgo</span>')
$body = (($body -split "`n") | Where-Object { $_ -notmatch '<\?php' }) -join "`n"
$body = $body.Trim("`r`n")

# 5.3 关于页：只保留「基于 csTimer」的极简介绍。
#     help.js 会读取 #about 的子元素来构建帮助面板，所以这里必须保持原来的层级结构。
$about = "<h1>Virgo</h1>`n<p>基于 csTimer（GPLv3）的安卓魔方计时器。</p>`n<p>内置 csTimer 版本：$version</p>"

$aboutDiv = '<div id="about" style="display:none;">' + "`n" + $about + "`n" + '</div>'
$body = $body.Replace('<div id="about" style="display:none;">' + "`n" + '</div>', $aboutDiv)
$aboutCount = ([regex]::Matches($body, 'id="about"')).Count
if ($aboutCount -ne 1) { throw "body 中的 #about 容器数量异常（$aboutCount），预期 1 个" }

$head = @'
<!DOCTYPE HTML>
<html class="p100">
 <head>
  <meta http-equiv="content-type" content="text/html; charset=UTF-8">
  <meta name="apple-mobile-web-app-capable" content="yes">
  <meta name="apple-mobile-web-app-status-bar-style" content="black">
  <meta name="format-detection" content="telephone=no">
  <meta name="viewport" content="width=device-width,initial-scale=1.0,maximum-scale=1.0,user-scalable=no,viewport-fit=cover">
  <link rel="apple-touch-icon" href="cstimer512x512.png">
  <title>Virgo - 魔方计时器</title>
  <script type="text/javascript">
var CSTIMER_VERSION = '__VERSION__';
var LANG_SET = '|zh-cn';
var LANG_STR = '简体中文';
var LANG_CUR = 'zh-cn';
  </script>
  <script type="text/javascript" src="lang/zh-cn.js"></script>
  <link rel='stylesheet' type='text/css' href='css/style.css'>
'@
$head = $head.Replace('__VERSION__', $version)

$tail = @'
<script type="text/javascript">
/* Virgo 下载桥：csTimer 使用 Blob + <a download> 导出文件，WebView 自身无法下载，
   这里统一拦截并交给原生 AndroidBridge.saveFile 落盘。 */
(function() {
	if (!window.AndroidBridge) return;
	document.addEventListener('click', function(e) {
		var el = e.target;
		while (el && el.tagName !== 'A') el = el.parentElement;
		if (!el || !el.getAttribute) return;
		if (!el.getAttribute('download')) return;
		var href = el.getAttribute('href') || '';
		if (href.indexOf('blob:') !== 0 && href.indexOf('data:') !== 0) return;
		e.preventDefault();
		e.stopPropagation();
		var name = el.getAttribute('download') || 'virgo.txt';
		fetch(href).then(function(r) {
			return r.blob();
		}).then(function(b) {
			var fr = new FileReader();
			fr.onload = function() {
				AndroidBridge.saveFile(name, String(fr.result).split(',')[1]);
			};
			fr.readAsDataURL(b);
		});
	}, true);
})();
</script>
</body>
</html>
'@

$nl = "`n"
$html = $head + $nl + ($scriptLines -join $nl) + $nl + ' </head>' + $nl + '<body>' + $nl + $body + $nl + $tail

Write-Utf8NoBom (Join-Path $outRoot 'index.html') $html

# ------------------------------------------------------------ 汇总
$files = Get-ChildItem -Recurse -File $outRoot
$total = ($files | Measure-Object -Property Length -Sum).Sum
Write-Host ''
Write-Host ("完成：{0} 个文件，共 {1:N1} MB" -f $files.Count, ($total / 1MB))
Write-Host "输出目录：$outRoot"