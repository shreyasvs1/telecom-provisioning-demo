-- INTERNET over FIBER: needs an ONT installed, a drop run if none exists, and a modem
INSERT INTO work_spec_catalog (service_code, network_type, work_spec_code, description, duration_minutes, required_skill)
VALUES ('INTERNET', 'FIBER', 'INSTALL_DROP', 'Run fiber drop from tap to premises', 60, 'OUTSIDE_PLANT');
INSERT INTO work_spec_catalog (service_code, network_type, work_spec_code, description, duration_minutes, required_skill)
VALUES ('INTERNET', 'FIBER', 'INSTALL_ONT', 'Install and activate Optical Network Terminal', 30, 'ELECTRONICS');
INSERT INTO work_spec_catalog (service_code, network_type, work_spec_code, description, duration_minutes, required_skill)
VALUES ('INTERNET', 'FIBER', 'INSTALL_MODEM', 'Install and configure fiber gateway/modem', 20, 'ELECTRONICS');

-- INTERNET over COPPER: needs a line test and a modem, no ONT
INSERT INTO work_spec_catalog (service_code, network_type, work_spec_code, description, duration_minutes, required_skill)
VALUES ('INTERNET', 'COPPER', 'TEST_LINE', 'Test copper line quality end to end', 25, 'OUTSIDE_PLANT');
INSERT INTO work_spec_catalog (service_code, network_type, work_spec_code, description, duration_minutes, required_skill)
VALUES ('INTERNET', 'COPPER', 'INSTALL_MODEM', 'Install and configure DSL modem', 20, 'ELECTRONICS');

-- PHONE over FIBER or COPPER: needs a jack connected and an ATA (analog telephone adapter) provisioned
INSERT INTO work_spec_catalog (service_code, network_type, work_spec_code, description, duration_minutes, required_skill)
VALUES ('PHONE', 'FIBER', 'CONNECT_JACK', 'Connect and test phone jack wiring', 15, 'INSIDE_WIRING');
INSERT INTO work_spec_catalog (service_code, network_type, work_spec_code, description, duration_minutes, required_skill)
VALUES ('PHONE', 'FIBER', 'PROVISION_ATA', 'Provision analog telephone adapter', 15, 'ELECTRONICS');
INSERT INTO work_spec_catalog (service_code, network_type, work_spec_code, description, duration_minutes, required_skill)
VALUES ('PHONE', 'COPPER', 'CONNECT_JACK', 'Connect and test phone jack wiring', 15, 'INSIDE_WIRING');

-- TV over FIBER: needs a set-top box installed and inside coax/HDMI wiring checked
INSERT INTO work_spec_catalog (service_code, network_type, work_spec_code, description, duration_minutes, required_skill)
VALUES ('TV', 'FIBER', 'INSTALL_SET_TOP_BOX', 'Install and activate set-top box', 25, 'ELECTRONICS');
INSERT INTO work_spec_catalog (service_code, network_type, work_spec_code, description, duration_minutes, required_skill)
VALUES ('TV', 'FIBER', 'CHECK_INSIDE_WIRING', 'Verify inside coax/HDMI wiring to TV location', 20, 'INSIDE_WIRING');
