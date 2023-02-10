locals {
  metric_period    = 300
  metric_namespace = "${var.environment}-gdx"
  metric_colours = {
    blue  = "#1f77b4"
    green = "#2ca02c"
    red   = "#d62728"
  }
}

resource "aws_cloudwatch_log_group" "ecs_logs" {
  name              = "${var.environment}-gdx-data-share-poc-ecs-logs"
  retention_in_days = var.cloudwatch_retention_period

  kms_key_id = aws_kms_key.log_key.arn
}

module "metrics_dashboard" {
  source           = "../cloudwatch_dashboard"
  region           = var.region
  dashboard_name   = "${var.environment}-metrics-dashboard"
  metric_namespace = local.metric_namespace
  widgets = [
    {
      title  = "Old API calls",
      period = local.metric_period,
      stat   = "Sum",
      metrics = [
        { name = "API_CALLS.IngestedEvents.count" },
        { name = "API_CALLS.CallsToLev.count" },
        { name = "API_CALLS.ResponsesFromLev.count" },
        { name = "API_CALLS.CallsToHmrc.count" },
        { name = "API_CALLS.ResponsesFromHmrc.count" },
        { name = "API_CALLS.CallsToPoll.count" },
        { name = "API_CALLS.CallsToEnrich.count" },
      ]
    },
    {
      title  = "Event API calls",
      period = local.metric_period,
      stat   = "Sum",
      metrics = [
        {
          name       = "API_CALLS.PublishEvent.count",
          dimensions = { success = true },
          attributes = { label = "PublishEvent" }
        },
        {
          name       = "API_CALLS.GetEvent.count",
          dimensions = { success = true },
          attributes = { label = "GetEvent" }
        },
        {
          name       = "API_CALLS.GetEvents.count",
          dimensions = { success = true },
          attributes = { label = "GetEvents" }
        },
        {
          name       = "API_CALLS.DeleteEvent.count",
          dimensions = { success = true },
          attributes = { label = "DeleteEvent" }
        },
      ]
    },
    {
      title  = "Data ingest calls",
      period = local.metric_period,
      stat   = "Sum",
      metrics = [
        {
          name       = "API_CALLS.PublishEvent.count",
          dimensions = { success = true },
          attributes = { label = "PublishEvent" }
        },
        {
          name       = "API_CALLS.CallsToLev.count",
          attributes = { label = "CallsToLev" }
        },
        {
          name       = "API_RESPONSES.ResponsesFromLev.count",
          attributes = { label = "ResponsesFromLev" }
        },
      ]
    },
    {
      title  = "Average data processing times",
      period = local.metric_period,
      stat   = "Average",
      metrics = [
        {
          name       = "DATA_PROCESSING.TimeFromCreationToDeletion.avg",
          attributes = { label = "Data creation to deletion time" }
        },
      ]
    },
    {
      title  = "Event actions",
      period = local.metric_period,
      stat   = "Sum",
      metrics = [
        {
          attributes = {
            expression = "SUM(SEARCH('{${local.metric_namespace},eventType} MetricName=\"EVENT_ACTION.EventPublished.count\"', 'Sum', 300))"
            color      = local.metric_colours.green
            label      = "Ingress events published"
          }
        },
        {
          attributes = {
            expression = "SUM(SEARCH('{${local.metric_namespace},consumerSubscription,eventType} MetricName=\"EVENT_ACTION.EventDeleted.count\"', 'Sum', 300))"
            color      = local.metric_colours.red
            label      = "Egress events deleted"
          }
        }
      ]
    }
  ]
}

locals {
  cost                   = var.environment == "dev" ? "£1234.56" : "£1234.56"
  completed_transactions = var.environment == "dev" ? "22,600" : "13"
  cost_per_transaction   = var.environment == "dev" ? "5.46p" : "£94.97"
}

resource "aws_cloudwatch_dashboard" "private_beta_metrics" {
  dashboard_name = "${var.environment}-private-beta-metrics"
  dashboard_body = jsonencode({
    widgets : [
      {
        type : "text",
        x : 0,
        y : 0,
        height : 6,
        width : 12,
        properties : {
          markdown : <<EOT
# Cost per transaction
## Definition

Our costs are the costs of AWS hosting and services.

Our number of completed transaction is the number of requests to mark an event as consumed.

Period | Cost | Completed transactions | Cost per transaction
-|-|-|-
3 Months | ${local.cost} | ${local.completed_transactions} | ${local.cost_per_transaction}

https://www.gov.uk/service-manual/measuring-success/measuring-cost-per-transaction
EOT
        }
      }
    ]
  })
}
