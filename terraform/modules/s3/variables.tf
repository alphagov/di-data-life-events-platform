variable "account_id" {
  type = string
}
variable "region" {
  type = string
}
variable "environment" {
  type = string
}
variable "name" {
  type = string
}
variable "expiration_days" {
  type    = number
  default = null
}

variable "use_kms" {
  type    = bool
  default = true
}

variable "sns_arn" {
  type = string
}
