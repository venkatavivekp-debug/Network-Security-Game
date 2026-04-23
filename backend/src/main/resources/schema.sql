CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    account_locked BOOLEAN NOT NULL DEFAULT FALSE,
    locked_until TIMESTAMP NULL,
    failed_login_attempts INT NOT NULL DEFAULT 0,
    last_login_at TIMESTAMP NULL,
    last_login_fingerprint_hash VARCHAR(64) NULL
);

ALTER TABLE users ADD COLUMN IF NOT EXISTS account_locked BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS locked_until TIMESTAMP NULL;
ALTER TABLE users ADD COLUMN IF NOT EXISTS failed_login_attempts INT NOT NULL DEFAULT 0;
ALTER TABLE users ADD COLUMN IF NOT EXISTS last_login_at TIMESTAMP NULL;
ALTER TABLE users ADD COLUMN IF NOT EXISTS last_login_fingerprint_hash VARCHAR(64) NULL;

CREATE TABLE IF NOT EXISTS messages (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    sender_id BIGINT NOT NULL,
    receiver_id BIGINT NOT NULL,
    encrypted_content LONGTEXT NOT NULL,
    original_hash VARCHAR(64) NULL,
    algorithm_type VARCHAR(20) NOT NULL,
    requested_algorithm_type VARCHAR(20) NULL,
    status VARCHAR(20) NOT NULL,
    risk_score_at_send DOUBLE NULL,
    risk_level_at_send VARCHAR(20) NULL,
    hold_reason VARCHAR(200) NULL,
    metadata LONGTEXT,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_messages_sender FOREIGN KEY (sender_id) REFERENCES users(id),
    CONSTRAINT fk_messages_receiver FOREIGN KEY (receiver_id) REFERENCES users(id)
);

-- Safe schema upgrades for existing databases (Spring runs schema.sql on startup).
ALTER TABLE messages ADD COLUMN IF NOT EXISTS original_hash VARCHAR(64) NULL;
ALTER TABLE messages ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'LOCKED';
ALTER TABLE messages ADD COLUMN IF NOT EXISTS requested_algorithm_type VARCHAR(20) NULL;
ALTER TABLE messages ADD COLUMN IF NOT EXISTS risk_score_at_send DOUBLE NULL;
ALTER TABLE messages ADD COLUMN IF NOT EXISTS risk_level_at_send VARCHAR(20) NULL;
ALTER TABLE messages ADD COLUMN IF NOT EXISTS hold_reason VARCHAR(200) NULL;

CREATE INDEX idx_messages_receiver_id ON messages(receiver_id);
CREATE INDEX idx_messages_sender_id ON messages(sender_id);
CREATE INDEX idx_messages_algorithm ON messages(algorithm_type);

CREATE TABLE IF NOT EXISTS puzzles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    message_id BIGINT NOT NULL UNIQUE,
    puzzle_type VARCHAR(30) NOT NULL,
    challenge VARCHAR(256) NOT NULL,
    target_hash VARCHAR(64) NOT NULL,
    max_iterations INT NOT NULL,
    wrapped_key LONGTEXT NOT NULL,
    attempts_allowed INT NOT NULL,
    attempts_used INT NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    solved_at TIMESTAMP NULL,
    solved_nonce INT NULL,
    CONSTRAINT fk_puzzles_message FOREIGN KEY (message_id) REFERENCES messages(id)
);

ALTER TABLE puzzles ADD COLUMN IF NOT EXISTS solved_nonce INT NULL;

CREATE TABLE IF NOT EXISTS audit_events (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    event_type VARCHAR(40) NOT NULL,
    actor_username VARCHAR(100) NULL,
    subject_username VARCHAR(100) NULL,
    ip_hash VARCHAR(64) NULL,
    fingerprint_hash VARCHAR(64) NULL,
    risk_score DOUBLE NULL,
    details LONGTEXT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_audit_events_created_at ON audit_events(created_at);
CREATE INDEX idx_audit_events_subject ON audit_events(subject_username);

CREATE INDEX idx_puzzles_message_id ON puzzles(message_id);

CREATE TABLE IF NOT EXISTS simulation_runs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    num_nodes INT NOT NULL,
    num_edges INT NOT NULL,
    attack_budget INT NOT NULL,
    defense_budget INT NOT NULL,
    recovery_budget INT NOT NULL,
    algorithm_type VARCHAR(20) NOT NULL,
    message_id BIGINT NULL,
    initial_connectivity DOUBLE NOT NULL,
    after_attack_connectivity DOUBLE NOT NULL,
    after_recovery_connectivity DOUBLE NOT NULL,
    nodes_lost INT NOT NULL,
    edges_lost INT NOT NULL,
    recovery_rate DOUBLE NOT NULL,
    defender_utility DOUBLE NOT NULL,
    attacker_utility DOUBLE NOT NULL,
    effective_attack_success_probability DOUBLE NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_sim_runs_created_at ON simulation_runs(created_at);
CREATE INDEX idx_sim_runs_algorithm ON simulation_runs(algorithm_type);

CREATE TABLE IF NOT EXISTS advanced_simulation_runs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    num_nodes INT NOT NULL,
    num_edges INT NOT NULL,
    attack_budget INT NOT NULL,
    defense_budget INT NOT NULL,
    recovery_budget INT NOT NULL,
    rounds INT NOT NULL,
    enable_mtd BOOLEAN NOT NULL,
    enable_deception BOOLEAN NOT NULL,
    algorithm_type VARCHAR(20) NOT NULL,
    seed_used BIGINT NOT NULL,
    compromise_timeline_json LONGTEXT NOT NULL,
    compromised_count_per_round_json LONGTEXT NOT NULL,
    round_details_json LONGTEXT NOT NULL,
    mean_time_to_compromise DOUBLE NOT NULL,
    max_attack_path_depth INT NOT NULL,
    resilience_score DOUBLE NOT NULL,
    defense_efficiency DOUBLE NOT NULL,
    attack_efficiency DOUBLE NOT NULL,
    deception_effectiveness DOUBLE NOT NULL,
    mtd_effectiveness DOUBLE NOT NULL,
    detection_rate DOUBLE NOT NULL,
    recovery_contribution DOUBLE NOT NULL,
    final_compromised_nodes INT NOT NULL,
    final_protected_nodes INT NOT NULL,
    attacker_utility DOUBLE NOT NULL,
    defender_utility DOUBLE NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_adv_sim_runs_created_at ON advanced_simulation_runs(created_at);
CREATE INDEX idx_adv_sim_runs_algorithm ON advanced_simulation_runs(algorithm_type);

CREATE TABLE IF NOT EXISTS evaluation_scenarios (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    scenario_name VARCHAR(120) NOT NULL,
    num_nodes INT NOT NULL,
    num_edges INT NOT NULL,
    attack_budget INT NOT NULL,
    defense_budget INT NOT NULL,
    recovery_budget INT NOT NULL,
    rounds INT NOT NULL,
    algorithm_type VARCHAR(20) NOT NULL,
    enable_mtd BOOLEAN NOT NULL,
    enable_deception BOOLEAN NOT NULL,
    repetitions INT NOT NULL,
    seed_strategy VARCHAR(20) NOT NULL,
    base_seed BIGINT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS evaluation_runs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    scenario_id BIGINT NULL,
    comparison_type VARCHAR(40) NOT NULL,
    scenario_name VARCHAR(120) NOT NULL,
    num_nodes INT NOT NULL,
    num_edges INT NOT NULL,
    attack_budget INT NOT NULL,
    defense_budget INT NOT NULL,
    recovery_budget INT NOT NULL,
    rounds INT NOT NULL,
    algorithm_type VARCHAR(20) NULL,
    enable_mtd BOOLEAN NOT NULL,
    enable_deception BOOLEAN NOT NULL,
    repetitions INT NOT NULL,
    seed_strategy VARCHAR(20) NOT NULL,
    base_seed BIGINT NULL,
    used_seeds_json LONGTEXT NOT NULL,
    average_final_compromised_nodes DOUBLE NOT NULL,
    average_compromise_ratio DOUBLE NOT NULL,
    average_resilience_score DOUBLE NOT NULL,
    average_attack_efficiency DOUBLE NOT NULL,
    average_defense_efficiency DOUBLE NOT NULL,
    average_deception_effectiveness DOUBLE NOT NULL,
    average_mtd_effectiveness DOUBLE NOT NULL,
    average_mean_time_to_compromise DOUBLE NOT NULL,
    average_attack_path_depth DOUBLE NOT NULL,
    std_dev_final_compromised_nodes DOUBLE NOT NULL,
    std_dev_compromise_ratio DOUBLE NOT NULL,
    std_dev_resilience_score DOUBLE NOT NULL,
    std_dev_attack_efficiency DOUBLE NOT NULL,
    std_dev_defense_efficiency DOUBLE NOT NULL,
    std_dev_mean_time_to_compromise DOUBLE NOT NULL,
    comparison_items_json LONGTEXT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_evaluation_runs_scenario FOREIGN KEY (scenario_id) REFERENCES evaluation_scenarios(id)
);

CREATE INDEX idx_eval_runs_created_at ON evaluation_runs(created_at);
CREATE INDEX idx_eval_runs_algorithm ON evaluation_runs(algorithm_type);
CREATE INDEX idx_eval_runs_type ON evaluation_runs(comparison_type);

CREATE TABLE IF NOT EXISTS evaluation_results (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    mode VARCHAR(20) NOT NULL,
    parameters_json LONGTEXT NOT NULL,
    metrics_json LONGTEXT NOT NULL,
    seed_used BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_eval_results_created_at ON evaluation_results(created_at);
CREATE INDEX idx_eval_results_mode ON evaluation_results(mode);
