resource "aws_ecs_cluster" "main" {
  name = var.environment

  setting {
    name  = "containerInsights"
    value = "enabled"
  }
}

resource "aws_ecs_task_definition" "gdx_data_share_poc" {
  family                   = "${var.environment}-gdx-data-share-poc"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  memory                   = 512
  cpu                      = 256
  execution_role_arn       = aws_iam_role.ecs_execution.arn
  task_role_arn            = aws_iam_role.ecs_task.arn

  container_definitions = jsonencode([
    {
      name         = "${var.environment}-gdx-data-share-poc",
      image        = "${var.ecr_url}/gdx-data-share-poc:${var.environment}",
      portMappings = [{ "containerPort" : 80, "hostPort" : 80 }],
      environment = [
        { "name" : "API_BASE_URL_LEV", "value" : var.lev_url },
        { "name" : "HMPPS_SQS_TOPICS_EVENT_ACCESS_KEY_ID", "value" : module.sns.access_key_id },
        { "name" : "HMPPS_SQS_TOPICS_EVENT_SECRET_ACCESS_KEY", "value" : module.sns.access_key_secret },
        { "name" : "HMPPS_SQS_TOPICS_EVENT_ARN", "value" : module.sns.sns_topic_arn }
      ]
      logConfiguration : {
        logDriver : "awslogs",
        options : {
          awslogs-group : aws_cloudwatch_log_group.ecs_logs.name,
          awslogs-region : var.region,
          awslogs-stream-prefix : "gdx-data-share-poc",
          awslogs-create-group : "true"
        }
      }
    }
  ])
}

resource "aws_ecs_service" "gdx_data_share_poc" {
  name                              = "${var.environment}-gdx-data-share-poc"
  cluster                           = aws_ecs_cluster.main.id
  task_definition                   = aws_ecs_task_definition.gdx_data_share_poc.arn
  launch_type                       = "FARGATE"
  desired_count                     = 2
  health_check_grace_period_seconds = 120

  lifecycle {
    ignore_changes = [
      desired_count,
      task_definition,
      load_balancer
    ]
  }

  deployment_controller {
    type = "CODE_DEPLOY"
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.green.arn
    container_name   = "${var.environment}-gdx-data-share-poc"
    container_port   = 80
  }

  network_configuration {
    security_groups = [aws_security_group.ecs_tasks.id]
    subnets         = module.vpc.private_subnet_ids
  }

  depends_on = [
    aws_security_group.ecs_tasks,
    aws_lb_target_group.green,
    aws_lb_listener.listener-http
  ]
}
