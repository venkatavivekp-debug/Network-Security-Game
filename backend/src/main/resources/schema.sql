CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL
);

CREATE TABLE IF NOT EXISTS messages (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    sender_id BIGINT NOT NULL,
    receiver_id BIGINT NOT NULL,
    encrypted_content LONGTEXT NOT NULL,
    algorithm_type VARCHAR(20) NOT NULL,
    metadata LONGTEXT,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_messages_sender FOREIGN KEY (sender_id) REFERENCES users(id),
    CONSTRAINT fk_messages_receiver FOREIGN KEY (receiver_id) REFERENCES users(id)
);

CREATE INDEX idx_messages_receiver_id ON messages(receiver_id);
CREATE INDEX idx_messages_sender_id ON messages(sender_id);
CREATE INDEX idx_messages_algorithm ON messages(algorithm_type);

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
