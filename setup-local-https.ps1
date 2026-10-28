$ErrorActionPreference = 'Stop'

$mkcert = Get-Command mkcert -ErrorAction SilentlyContinue
if (-not $mkcert) {
    throw 'Instale o mkcert para Windows e execute este script novamente.'
}

$rotaPadrao = Get-NetRoute -AddressFamily IPv4 -DestinationPrefix '0.0.0.0/0' |
    Sort-Object RouteMetric |
    Select-Object -First 1
if (-not $rotaPadrao) {
    throw 'Não foi possível detectar uma conexão de rede ativa.'
}

$ipLocal = Get-NetIPAddress -AddressFamily IPv4 -InterfaceIndex $rotaPadrao.InterfaceIndex |
    Where-Object { $_.IPAddress -notlike '169.254.*' -and $_.IPAddress -ne '127.0.0.1' } |
    Select-Object -First 1 -ExpandProperty IPAddress
if (-not $ipLocal) {
    throw 'Não foi possível detectar o endereço IPv4 da rede local.'
}
$angularJson = Get-Content (Join-Path $PSScriptRoot 'Frontend\frota-app\angular.json') -Raw
if ($angularJson -notmatch [regex]::Escape($ipLocal)) {
    Write-Warning "O IP detectado ($ipLocal) não está em allowedHosts no angular.json. Adicione-o antes de iniciar o frontend."
}

$pastaCertificados = Join-Path $PSScriptRoot 'Frontend\frota-app\certs'
New-Item -ItemType Directory -Path $pastaCertificados -Force | Out-Null

& $mkcert.Source -install
if ($LASTEXITCODE -ne 0) {
    throw 'O mkcert não conseguiu instalar a autoridade certificadora local no Windows.'
}

& $mkcert.Source `
    -cert-file (Join-Path $pastaCertificados 'frota.pem') `
    -key-file (Join-Path $pastaCertificados 'frota-key.pem') `
    localhost 127.0.0.1 $ipLocal
if ($LASTEXITCODE -ne 0) {
    throw 'O mkcert não conseguiu criar o certificado HTTPS.'
}

Write-Output "Certificado HTTPS criado para localhost e $ipLocal."
Write-Output "No celular, importe o certificado raiz indicado por: mkcert -CAROOT"
Write-Output 'Copie rootCA.pem para o celular e instale-o como certificado de CA confiável.'
Write-Output 'Reinicie o frontend e o backend com as instruções HTTPS do README.'
