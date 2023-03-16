resource "aws_securityhub_standards_control" "controls" {
  for_each = {
    for index, rule in var.rules :
    index => rule
  }
  standards_control_arn = "arn:aws:securityhub:${var.region}:${var.account_id}:security-control/${each.value.rule}"
  control_status        = "DISABLED"
  disabled_reason       = each.value.disabled_reason
}
