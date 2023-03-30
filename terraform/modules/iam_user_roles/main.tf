locals {
  admin_users     = tomap(var.admin_users)
  read_only_users = tomap(var.read_only_users)
}

data "aws_iam_policy" "admin_policy" {
  name = "AdministratorAccess"
}

data "aws_iam_policy" "read_only_policy" {
  name = "ReadOnlyAccess"
}

module "admin_roles" {
  for_each = local.admin_users

  source      = "../iam_user_role"
  role_suffix = "admin"
  username    = each.value
  policy_arn  = data.aws_iam_policy.admin_policy.arn
}

module "read_only_roles" {
  for_each = local.read_only_users

  source      = "../iam_user_role"
  role_suffix = "read-only"
  username    = each.value
  policy_arn  = data.aws_iam_policy.read_only_policy.arn
}
