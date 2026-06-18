param(
    [string]$ApiBaseUrl = "https://api.anyibj.cn"
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($env:ANYI_ADMIN_TOKEN)) {
    throw "Please set ANYI_ADMIN_TOKEN to an admin Bearer token before processing the asset delete queue."
}

$headers = @{
    Authorization = "Bearer $env:ANYI_ADMIN_TOKEN"
}

$response = Invoke-RestMethod `
    -Uri "$ApiBaseUrl/admin/asset-delete-queue/process" `
    -Method Post `
    -Headers $headers

$response | ConvertTo-Json -Depth 5
