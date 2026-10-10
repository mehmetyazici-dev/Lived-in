# Creates an isolated flat QA save using only a copy of the existing development
# save's level metadata. No existing chunks, entities, or player files are copied.
param(
    [string]$SaveDirectory = 'C:\Lived-in\runs\package-smoke\saves\Lived-In Phase 1 QA',
    [string]$TemplateLevel = 'C:\Lived-in\run\saves\Lived-In Milestone 1\level.dat'
)
$ErrorActionPreference = 'Stop'
$encoding = [Text.UTF8Encoding]::new($false)
if (-not (Test-Path -LiteralPath (Join-Path $SaveDirectory 'level.dat'))) {
    New-Item -ItemType Directory -Path $SaveDirectory -Force | Out-Null
    $inputFile = [IO.File]::OpenRead($TemplateLevel)
    $gzip = [IO.Compression.GZipStream]::new($inputFile, [IO.Compression.CompressionMode]::Decompress)
    $buffer = [IO.MemoryStream]::new()
    $gzip.CopyTo($buffer); $gzip.Dispose(); $inputFile.Dispose()
    $bytes = $buffer.ToArray(); $buffer.Dispose()
    $marker = [byte[]](8,0,9) + $encoding.GetBytes('LevelName')
    $offset = -1
    for ($i=0; $i -lt $bytes.Length-$marker.Length; $i++) {
        $match=$true
        for ($j=0; $j -lt $marker.Length; $j++) { if ($bytes[$i+$j] -ne $marker[$j]) { $match=$false; break } }
        if ($match) { $offset=$i+$marker.Length; break }
    }
    if ($offset -lt 0) { throw 'Template has no LevelName field.' }
    $length = 256 * [int]$bytes[$offset] + [int]$bytes[$offset+1]
    $name = $encoding.GetBytes('Lived-In Phase 1 QA')
    $updated = $bytes[0..($offset-1)] + [byte[]](0,$name.Length) + $name + $bytes[($offset+2+$length)..($bytes.Length-1)]
    $outputFile=[IO.File]::Create((Join-Path $SaveDirectory 'level.dat'))
    $gzip=[IO.Compression.GZipStream]::new($outputFile,[IO.Compression.CompressionMode]::Compress)
    $gzip.Write([byte[]]$updated,0,$updated.Length); $gzip.Dispose(); $outputFile.Dispose()
}
$pack = Join-Path $SaveDirectory 'datapacks\phase1-qa'
$functions = Join-Path $pack 'data\phase1qa\function'
New-Item -ItemType Directory -Path $functions -Force | Out-Null
[IO.File]::WriteAllText((Join-Path $pack 'pack.mcmeta'),'{"pack":{"pack_format":48,"description":"Local Lived-In Phase 1 QA helpers"}}',$encoding)
function Write-QAFunction([string]$Name,[string[]]$Lines) {
    [IO.File]::WriteAllText((Join-Path $functions "$Name.mcfunction"),($Lines -join "`n")+"`n",$encoding)
}
Write-QAFunction 'setup' @('execute unless data storage phase1qa:state initialized run function phase1qa:create')
$setup = @('difficulty peaceful','gamerule doMobSpawning false','gamerule doDaylightCycle false','gamerule spawnChunkRadius 0','time set day','tp @s 26 -60 10 180 10')
foreach ($x in @(0,20,40)) {
    $setup += "livedin camp $($x+6) -60 6"
    $setup += "fill $($x+15) -61 8 $($x+19) -61 12 minecraft:water"
    $setup += "livedin fishing $($x+17) -60 6 $($x+17) -61 10"
    $setup += "fill $($x+3) -61 13 $($x+15) -61 17 minecraft:water"
    $setup += "livedin boat $($x+5) -61 15 $($x+13) -61 15"
}
$setup += @('data modify storage phase1qa:state initialized set value 1b','function phase1qa:check')
Write-QAFunction 'create' $setup
$check = @('scoreboard objectives add phase1qa dummy','data modify storage phase1qa:state ids set value []')
foreach ($type in @('livedin:ambient_npc','minecraft:cat','minecraft:boat','minecraft:fishing_bobber')) {
    $key = ($type -split ':')[1]
    $selector = "@e[type=$type]"
    if ($type -eq 'minecraft:cat') { $selector = '@e[type=minecraft:cat,nbt={Sitting:1b}]' }
    $check += "scoreboard players set $key phase1qa 0"
    $check += "execute as $selector run scoreboard players add $key phase1qa 1"
    $check += "execute as $selector run data modify storage phase1qa:state ids append from entity @s UUID"
    $check += "tellraw @s [{`"text`":`"Phase1QA $key = `"},{`"score`":{`"name`":`"$key`",`"objective`":`"phase1qa`"}}]"
}
$check += @('tellraw @s [{"text":"Phase1QA IDs: "},{"nbt":"ids","storage":"phase1qa:state"}]','execute as @e[type=livedin:ambient_npc,nbt={NpcRole:3}] run tellraw @a [{"text":"Phase1QA fisher pet: "},{"nbt":"PetCat","entity":"@s"}]','execute as @e[type=minecraft:cat,nbt={Sitting:1b}] run tellraw @a [{"text":"Phase1QA cat owner: "},{"nbt":"Owner","entity":"@s"}]','execute as @e[type=livedin:ambient_npc,nbt={NpcRole:4}] run tellraw @a [{"text":"Phase1QA rider boat: "},{"nbt":"SceneBoat","entity":"@s"}]','execute as @e[type=minecraft:boat] run tellraw @a [{"text":"Phase1QA boat passenger: "},{"nbt":"Passengers[0].UUID","entity":"@s"}]')
Write-QAFunction 'check' $check
# /debug requires permission level 3; datapack functions have level 2. Run
# /debug start and /debug stop directly in player chat for timing evidence.
Write-QAFunction 'leave' @('tp @s 2000 -60 2000','schedule function phase1qa:return 35s replace')
$return = @()
foreach ($cx in 0..3) { foreach ($cz in 0..1) {
    $return += "execute if loaded $($cx*16) -60 $($cz*16) run tellraw @a {`"text`":`"Phase1QA ERROR: scene chunk $cx,$cz still loaded`"}"
    $return += "execute unless loaded $($cx*16) -60 $($cz*16) run tellraw @a {`"text`":`"Phase1QA unloaded chunk $cx,$cz`"}"
} }
$return += @('tp @a 26 -60 10 180 10','schedule function phase1qa:revisited 5s replace')
Write-QAFunction 'return' $return
Write-QAFunction 'revisited' @('execute as @a run function phase1qa:check')
Write-Host "QA save ready: $SaveDirectory"
Write-Host 'Open the save, then run /function phase1qa:setup. Checks: check, leave.'
