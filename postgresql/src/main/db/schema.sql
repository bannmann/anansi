create table if not exists fingerprint
(
    id varchar(8) not null,
    name varchar(256) not null,
    location varchar(512) null,
    frames varchar(8192) not null,

    /* dump of extra data used for fingerprinting (= id). also included as incident data. */
    extra_data varchar(4096) not null,

    constraint fingerprint_pk
        primary key (id)
);

create table if not exists incident
(
    id varchar(32) not null,
    timestamp timestamp with time zone not null,
    fingerprint_id varchar(8) not null,
    severity varchar(32) not null,
    throwable_details varchar(16384) not null,
    build varchar(64) not null,

    constraint incident_pk
        primary key (id)
);

/* stores both extra data used for fingerprinting and context data logged for the incident. */
create table if not exists incident_data
(
    incident_id varchar(32) not null,
    key varchar(64) not null,
    value varchar(16384) not null,

    constraint incidentdata_pk
        primary key (incident_id, key),
    constraint incidentdata_fk_incidentid
        foreign key (incident_id)
            references incident (id)
            on delete cascade
);
