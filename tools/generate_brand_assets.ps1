using namespace System.Drawing
using namespace System.Drawing.Drawing2D
using namespace System.Drawing.Imaging
using namespace System.Drawing.Text

Add-Type -AssemblyName System.Drawing

$ErrorActionPreference = 'Stop'

function New-RoundedRectanglePath {
    param(
        [float]$X,
        [float]$Y,
        [float]$Width,
        [float]$Height,
        [float]$Radius
    )

    $diameter = $Radius * 2
    $path = [GraphicsPath]::new()
    $path.AddArc($X, $Y, $diameter, $diameter, 180, 90)
    $path.AddArc($X + $Width - $diameter, $Y, $diameter, $diameter, 270, 90)
    $path.AddArc($X + $Width - $diameter, $Y + $Height - $diameter, $diameter, $diameter, 0, 90)
    $path.AddArc($X, $Y + $Height - $diameter, $diameter, $diameter, 90, 90)
    $path.CloseFigure()
    return $path
}

function New-Graphics {
    param([Bitmap]$Bitmap)

    $graphics = [Graphics]::FromImage($Bitmap)
    $graphics.SmoothingMode = [SmoothingMode]::AntiAlias
    $graphics.InterpolationMode = [InterpolationMode]::HighQualityBicubic
    $graphics.PixelOffsetMode = [PixelOffsetMode]::HighQuality
    $graphics.CompositingQuality = [CompositingQuality]::HighQuality
    $graphics.TextRenderingHint = [TextRenderingHint]::AntiAliasGridFit
    return $graphics
}

function Add-TextPath {
    param(
        [GraphicsPath]$Path,
        [string]$Text,
        [string]$FontFamilyName,
        [int]$Style,
        [float]$EmSize,
        [float]$X,
        [float]$Y
    )

    $family = [FontFamily]::new($FontFamilyName)
    try {
        $origin = [PointF]::new($X, $Y)
        $format = [StringFormat]::GenericDefault
        $Path.AddString($Text, $family, $Style, $EmSize, $origin, $format)
    }
    finally {
        $family.Dispose()
    }
}

function Draw-FilledStrokedPath {
    param(
        [Graphics]$Graphics,
        [GraphicsPath]$Path,
        [Color]$FillColor,
        [Color]$StrokeColor,
        [float]$StrokeWidth
    )

    $fill = [SolidBrush]::new($FillColor)
    $stroke = [Pen]::new($StrokeColor, $StrokeWidth)
    $stroke.LineJoin = [LineJoin]::Round

    try {
        $Graphics.FillPath($fill, $Path)
        if ($StrokeWidth -gt 0) {
            $Graphics.DrawPath($stroke, $Path)
        }
    }
    finally {
        $fill.Dispose()
        $stroke.Dispose()
    }
}

function Save-ScaledPng {
    param(
        [Bitmap]$Source,
        [int]$Size,
        [string]$Path
    )

    $target = [Bitmap]::new($Size, $Size)
    $graphics = New-Graphics -Bitmap $target

    try {
        $graphics.Clear([Color]::Transparent)
        $graphics.DrawImage($Source, 0, 0, $Size, $Size)
        $directory = Split-Path -Parent $Path
        if (-not (Test-Path $directory)) {
            New-Item -ItemType Directory -Force -Path $directory | Out-Null
        }
        $target.Save($Path, [ImageFormat]::Png)
    }
    finally {
        $graphics.Dispose()
        $target.Dispose()
    }
}

$root = Split-Path -Parent $PSScriptRoot
$resRoot = Join-Path $root 'app\src\main\res'
$drawableNoDpi = Join-Path $resRoot 'drawable-nodpi'
$tempRoot = Join-Path $root 'tmp'

New-Item -ItemType Directory -Force -Path $drawableNoDpi | Out-Null
New-Item -ItemType Directory -Force -Path $tempRoot | Out-Null

$blue = [Color]::FromArgb(255, 12, 69, 166)
$blueDark = [Color]::FromArgb(255, 9, 43, 104)
$blueMid = [Color]::FromArgb(255, 23, 87, 196)
$orange = [Color]::FromArgb(255, 247, 128, 15)
$orangeLight = [Color]::FromArgb(255, 255, 164, 34)
$white = [Color]::FromArgb(255, 249, 248, 244)
$cream = [Color]::FromArgb(255, 255, 248, 236)
$shadow = [Color]::FromArgb(70, 7, 22, 58)

$bold = [int][FontStyle]::Bold
$boldItalic = [int]([FontStyle]::Bold -bor [FontStyle]::Italic)

# Launcher master icon
$iconMaster = [Bitmap]::new(1024, 1024)
$g = New-Graphics -Bitmap $iconMaster

try {
    $g.Clear([Color]::Transparent)

    $shadowPath = New-RoundedRectanglePath -X 146 -Y 168 -Width 732 -Height 732 -Radius 210
    $shadowBrush = [SolidBrush]::new($shadow)
    $g.FillPath($shadowBrush, $shadowPath)
    $shadowBrush.Dispose()
    $shadowPath.Dispose()

    $cardPath = New-RoundedRectanglePath -X 128 -Y 128 -Width 768 -Height 768 -Radius 210
    $gradient = [LinearGradientBrush]::new([PointF]::new(128, 128), [PointF]::new(896, 896), $blueMid, $blueDark)
    $g.FillPath($gradient, $cardPath)
    $gradient.Dispose()

    $cardPen = [Pen]::new([Color]::FromArgb(255, 5, 34, 88), 14)
    $cardPen.LineJoin = [LineJoin]::Round
    $g.DrawPath($cardPen, $cardPath)
    $cardPen.Dispose()
    $cardPath.Dispose()

    $motionBrush = [SolidBrush]::new([Color]::FromArgb(255, 244, 109, 14))
    $motion = [Point[]]@(
        [Point]::new(148, 585),
        [Point]::new(324, 520),
        [Point]::new(234, 646),
        [Point]::new(388, 596),
        [Point]::new(236, 744),
        [Point]::new(140, 694)
    )
    $g.FillPolygon($motionBrush, $motion)
    $motionBrush.Dispose()

    $accentBrush = [SolidBrush]::new([Color]::FromArgb(255, 255, 172, 49))
    $accent = [Point[]]@(
        [Point]::new(156, 548),
        [Point]::new(266, 508),
        [Point]::new(218, 576),
        [Point]::new(320, 542),
        [Point]::new(214, 650),
        [Point]::new(164, 630)
    )
    $g.FillPolygon($accentBrush, $accent)
    $accentBrush.Dispose()

    $xPath = [GraphicsPath]::new()
    Add-TextPath -Path $xPath -Text 'X' -FontFamilyName 'Segoe UI' -Style $boldItalic -EmSize 410 -X 205 -Y 178
    Draw-FilledStrokedPath -Graphics $g -Path $xPath -FillColor $white -StrokeColor $blueDark -StrokeWidth 18
    $xPath.Dispose()

    $gPath = [GraphicsPath]::new()
    Add-TextPath -Path $gPath -Text 'G' -FontFamilyName 'Segoe UI' -Style $boldItalic -EmSize 300 -X 505 -Y 270
    Draw-FilledStrokedPath -Graphics $g -Path $gPath -FillColor $orange -StrokeColor $blueDark -StrokeWidth 18
    $gPath.Dispose()

    $goPath = [GraphicsPath]::new()
    Add-TextPath -Path $goPath -Text 'o' -FontFamilyName 'Segoe UI' -Style $boldItalic -EmSize 220 -X 695 -Y 338
    Draw-FilledStrokedPath -Graphics $g -Path $goPath -FillColor $orange -StrokeColor $blueDark -StrokeWidth 14
    $goPath.Dispose()

    $pillPath = New-RoundedRectanglePath -X 182 -Y 722 -Width 664 -Height 126 -Radius 58
    $pillBrush = [SolidBrush]::new($orange)
    $g.FillPath($pillBrush, $pillPath)
    $pillBrush.Dispose()
    $pillPen = [Pen]::new($cream, 8)
    $pillPen.LineJoin = [LineJoin]::Round
    $g.DrawPath($pillPen, $pillPath)
    $pillPen.Dispose()
    $pillPath.Dispose()

    $pitPath = [GraphicsPath]::new()
    Add-TextPath -Path $pitPath -Text 'PITALITO' -FontFamilyName 'Segoe UI' -Style $boldItalic -EmSize 92 -X 264 -Y 728
    Draw-FilledStrokedPath -Graphics $g -Path $pitPath -FillColor $white -StrokeColor $orange -StrokeWidth 4
    $pitPath.Dispose()
}
finally {
    $g.Dispose()
}

$iconMasterPath = Join-Path $tempRoot 'xpertgo_icon_master.png'
$iconMaster.Save($iconMasterPath, [ImageFormat]::Png)

$sizes = @{
    'mipmap-mdpi' = 48
    'mipmap-hdpi' = 72
    'mipmap-xhdpi' = 96
    'mipmap-xxhdpi' = 144
    'mipmap-xxxhdpi' = 192
}

foreach ($entry in $sizes.GetEnumerator()) {
    $dir = Join-Path $resRoot $entry.Key
    Save-ScaledPng -Source $iconMaster -Size $entry.Value -Path (Join-Path $dir 'ic_launcher.png')
    Save-ScaledPng -Source $iconMaster -Size $entry.Value -Path (Join-Path $dir 'ic_launcher_round.png')
}

$iconMaster.Dispose()

# Brand banner for welcome screen
$banner = [Bitmap]::new(1500, 700)
$bg = New-Graphics -Bitmap $banner

try {
    $bg.Clear([Color]::Transparent)

    $blobBrush = [SolidBrush]::new([Color]::FromArgb(255, 14, 79, 175))
    $bg.FillEllipse($blobBrush, 20, 70, 340, 500)
    $bg.FillEllipse($blobBrush, 105, 20, 260, 360)
    $bg.FillEllipse($blobBrush, 120, 260, 180, 300)
    $blobBrush.Dispose()

    $tailBrush = [SolidBrush]::new($orange)
    $tail = [Point[]]@(
        [Point]::new(40, 370),
        [Point]::new(160, 390),
        [Point]::new(98, 425),
        [Point]::new(212, 438),
        [Point]::new(110, 474),
        [Point]::new(32, 450)
    )
    $bg.FillPolygon($tailBrush, $tail)
    $tailBrush.Dispose()

    $markImage = [Image]::FromFile($iconMasterPath)
    try {
        $bg.DrawImage($markImage, 40, 120, 310, 310)
    }
    finally {
        $markImage.Dispose()
    }

    $wordShadow = [GraphicsPath]::new()
    Add-TextPath -Path $wordShadow -Text 'Xpert' -FontFamilyName 'Segoe UI' -Style $boldItalic -EmSize 240 -X 350 -Y 120
    Draw-FilledStrokedPath -Graphics $bg -Path $wordShadow -FillColor $white -StrokeColor $blueDark -StrokeWidth 16
    $wordShadow.Dispose()

    $goBanner = [GraphicsPath]::new()
    Add-TextPath -Path $goBanner -Text 'Go' -FontFamilyName 'Segoe UI' -Style $boldItalic -EmSize 240 -X 980 -Y 120
    Draw-FilledStrokedPath -Graphics $bg -Path $goBanner -FillColor $orange -StrokeColor $blueDark -StrokeWidth 16
    $goBanner.Dispose()

    $bannerPath = New-RoundedRectanglePath -X 520 -Y 395 -Width 690 -Height 140 -Radius 62
    $bannerBrush = [SolidBrush]::new($orange)
    $bg.FillPath($bannerBrush, $bannerPath)
    $bannerBrush.Dispose()
    $bannerPath.Dispose()

    $pitBanner = [GraphicsPath]::new()
    Add-TextPath -Path $pitBanner -Text 'PITALITO' -FontFamilyName 'Segoe UI' -Style $boldItalic -EmSize 110 -X 618 -Y 399
    Draw-FilledStrokedPath -Graphics $bg -Path $pitBanner -FillColor $white -StrokeColor $orange -StrokeWidth 4
    $pitBanner.Dispose()
}
finally {
    $bg.Dispose()
}

$bannerPath = Join-Path $drawableNoDpi 'app_brand_banner.png'
$banner.Save($bannerPath, [ImageFormat]::Png)
$banner.Dispose()

Write-Output "Brand assets generated in $resRoot"
