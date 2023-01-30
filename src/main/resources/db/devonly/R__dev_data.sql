CREATE OR REPLACE FUNCTION getIdFromPublisherName(publisher_name_check varchar(80))
    RETURNS UUID
    LANGUAGE plpgsql
AS
$$
Declare
    publisher_id UUID;
Begin
    SELECT id
    INTO publisher_id
    FROM publisher
    WHERE name = publisher_name_check;
    RETURN publisher_id;
End;
$$;

CREATE OR REPLACE FUNCTION getIdFromConsumerName(consumer_name_check varchar(80))
    RETURNS UUID
    LANGUAGE plpgsql
AS
$$
Declare
    consumer_id UUID;
Begin
    SELECT id
    INTO consumer_id
    FROM consumer
    WHERE name = consumer_name_check;
    RETURN consumer_id;
End;
$$;

DELETE
FROM publisher_subscription
WHERE client_id IN ('len', 'internal-inbound', 'internal-inbound');

DELETE
FROM event_data
WHERE consumer_subscription_id IN (SELECT id
                                   FROM consumer_subscription
                                   WHERE oauth_client_id IN ('dwp-event-receiver', 'hmrc-client', 'internal-outbound'));

DELETE
FROM consumer_subscription
WHERE oauth_client_id IN ('dwp-event-receiver', 'hmrc-client', 'internal-outbound');

DELETE
FROM consumer_subscription_enrichment_field
WHERE enrichment_field IN
      ('registrationDate', 'firstNames', 'lastName', 'maidenName', 'dateOfDeath', 'dateOfBirth', 'sex', 'address',
       'birthplace', 'deathplace', 'occupation', 'retired');

INSERT INTO publisher_subscription
    (client_id, publisher_id, event_type)
VALUES ('len', getIdFromPublisherName('HMPO'), 'DEATH_NOTIFICATION');

INSERT INTO consumer_subscription
(oauth_client_id, consumer_id, event_type, enrichment_fields_included_in_poll)
VALUES ('dwp-event-receiver', getIdFromConsumerName('DWP Poller'),
        'DEATH_NOTIFICATION', false),
       ('dwp-event-receiver', getIdFromConsumerName('DWP Poller'), 'LIFE_EVENT', false);

INSERT INTO consumer_subscription
(oauth_client_id, consumer_id, event_type, enrichment_fields_included_in_poll)
VALUES ('hmrc-client', getIdFromConsumerName('Pub/Sub Consumer'),
        'DEATH_NOTIFICATION', true),
       ('hmrc-client', getIdFromConsumerName('Pub/Sub Consumer'), 'LIFE_EVENT', true);

INSERT INTO consumer_subscription_enrichment_field(consumer_subscription_id, enrichment_field)
SELECT id,
       unnest(ARRAY ['registrationDate', 'firstNames', 'lastName', 'maidenName', 'dateOfDeath', 'dateOfBirth', 'sex', 'address', 'birthplace', 'deathplace', 'occupation', 'retired'])
FROM consumer_subscription
WHERE event_type = 'DEATH_NOTIFICATION';

DROP FUNCTION IF EXISTS getIdFromPublisherName;
DROP FUNCTION IF EXISTS getIdFromConsumerName;
