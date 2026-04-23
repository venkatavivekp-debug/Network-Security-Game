package backend.config;

import backend.dto.MessageSendRequest;
import backend.dto.RegisterRequest;
import backend.dto.AdvancedSimulationRunRequest;
import backend.dto.EvaluationScenarioRequest;
import backend.model.AlgorithmType;
import backend.model.EvaluationSeedStrategy;
import backend.model.Role;
import backend.repository.AdvancedSimulationRunRepository;
import backend.repository.EvaluationRunRepository;
import backend.repository.MessageRepository;
import backend.repository.SimulationRunRepository;
import backend.repository.UserRepository;
import backend.service.AdvancedSimulationService;
import backend.service.EvaluationFrameworkService;
import backend.service.MessageService;
import backend.service.SimulationHistoryService;
import backend.service.UserService;
import backend.simulation.game.GameSimulationService;
import backend.simulation.game.SimulationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataSeeder.class);

    private final DemoDataProperties demoDataProperties;
    private final UserRepository userRepository;
    private final MessageRepository messageRepository;
    private final SimulationRunRepository simulationRunRepository;
    private final AdvancedSimulationRunRepository advancedSimulationRunRepository;
    private final EvaluationRunRepository evaluationRunRepository;
    private final UserService userService;
    private final MessageService messageService;
    private final GameSimulationService gameSimulationService;
    private final SimulationHistoryService simulationHistoryService;
    private final AdvancedSimulationService advancedSimulationService;
    private final EvaluationFrameworkService evaluationFrameworkService;

    public DataSeeder(
            DemoDataProperties demoDataProperties,
            UserRepository userRepository,
            MessageRepository messageRepository,
            SimulationRunRepository simulationRunRepository,
            AdvancedSimulationRunRepository advancedSimulationRunRepository,
            EvaluationRunRepository evaluationRunRepository,
            UserService userService,
            MessageService messageService,
            GameSimulationService gameSimulationService,
            SimulationHistoryService simulationHistoryService,
            AdvancedSimulationService advancedSimulationService,
            EvaluationFrameworkService evaluationFrameworkService
    ) {
        this.demoDataProperties = demoDataProperties;
        this.userRepository = userRepository;
        this.messageRepository = messageRepository;
        this.simulationRunRepository = simulationRunRepository;
        this.advancedSimulationRunRepository = advancedSimulationRunRepository;
        this.evaluationRunRepository = evaluationRunRepository;
        this.userService = userService;
        this.messageService = messageService;
        this.gameSimulationService = gameSimulationService;
        this.simulationHistoryService = simulationHistoryService;
        this.advancedSimulationService = advancedSimulationService;
        this.evaluationFrameworkService = evaluationFrameworkService;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!demoDataProperties.isEnabled()) {
            LOGGER.info("Demo data seeding disabled (app.seed-demo-data.enabled=false)");
            return;
        }

        if (!isValidConfig()) {
            LOGGER.warn("Demo data seeding skipped due to invalid demo credentials configuration");
            return;
        }

        LOGGER.info("Demo data seeding started");
        seedUsers();
        seedMessages();
        seedSimulations();
        seedAdvancedSimulations();
        seedEvaluationRuns();
        LOGGER.info("Demo data seeding completed");
    }

    private boolean isValidConfig() {
        return notBlank(demoDataProperties.getSenderUsername())
                && notBlank(demoDataProperties.getSenderPassword())
                && notBlank(demoDataProperties.getReceiverUsername())
                && notBlank(demoDataProperties.getReceiverPassword());
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private void seedUsers() {
        if (!userRepository.existsByUsername(demoDataProperties.getSenderUsername())) {
            RegisterRequest sender = new RegisterRequest();
            sender.setUsername(demoDataProperties.getSenderUsername());
            sender.setPassword(demoDataProperties.getSenderPassword());
            sender.setRole(Role.SENDER);
            userService.register(sender);
            LOGGER.info("Seeded demo sender user '{}'.", demoDataProperties.getSenderUsername());
        }

        if (!userRepository.existsByUsername(demoDataProperties.getReceiverUsername())) {
            RegisterRequest receiver = new RegisterRequest();
            receiver.setUsername(demoDataProperties.getReceiverUsername());
            receiver.setPassword(demoDataProperties.getReceiverPassword());
            receiver.setRole(Role.RECEIVER);
            userService.register(receiver);
            LOGGER.info("Seeded demo receiver user '{}'.", demoDataProperties.getReceiverUsername());
        }
    }

    private void seedMessages() {
        if (messageRepository.count() > 0) {
            LOGGER.info("Skipping demo message seed because messages already exist");
            return;
        }

        createDemoMessage("Demo NORMAL secured message", AlgorithmType.NORMAL);
        createDemoMessage("Demo SHCS metadata-hidden message", AlgorithmType.SHCS);
        createDemoMessage("Demo CPHS puzzle-protected message", AlgorithmType.CPHS);
    }

    private void createDemoMessage(String content, AlgorithmType algorithmType) {
        MessageSendRequest request = new MessageSendRequest();
        request.setReceiverUsername(demoDataProperties.getReceiverUsername());
        request.setContent(content);
        request.setAlgorithmType(algorithmType);

        messageService.sendMessage(demoDataProperties.getSenderUsername(), request);
        LOGGER.info("Seeded demo message using {}", algorithmType);
    }

    private void seedSimulations() {
        if (simulationRunRepository.count() > 0) {
            LOGGER.info("Skipping demo simulation seed because simulation runs already exist");
            return;
        }

        int numNodes = 10;
        int numEdges = 15;
        int attackBudget = 3;
        int defenseBudget = 3;
        int recoveryBudget = 2;

        for (AlgorithmType algorithmType : AlgorithmType.values()) {
            SimulationResult result = gameSimulationService.runSimulation(
                    numNodes,
                    numEdges,
                    attackBudget,
                    defenseBudget,
                    recoveryBudget,
                    algorithmType
            );

            simulationHistoryService.saveRun(
                    numNodes,
                    numEdges,
                    attackBudget,
                    defenseBudget,
                    recoveryBudget,
                    algorithmType,
                    null,
                    result
            );
            LOGGER.info("Seeded demo simulation run for {}", algorithmType);
        }
    }

    private void seedAdvancedSimulations() {
        if (advancedSimulationRunRepository.count() > 0) {
            LOGGER.info("Skipping demo advanced simulation seed because advanced simulation runs already exist");
            return;
        }

        AdvancedSimulationRunRequest request = new AdvancedSimulationRunRequest();
        request.setNumNodes(20);
        request.setNumEdges(35);
        request.setAttackBudget(6);
        request.setDefenseBudget(6);
        request.setRecoveryBudget(3);
        request.setRounds(10);
        request.setEnableMTD(true);
        request.setEnableDeception(true);
        request.setAlgorithmType(AlgorithmType.CPHS);
        request.setSeed(42L);

        advancedSimulationService.runAndPersist(request);
        LOGGER.info("Seeded demo advanced simulation run");
    }

    private void seedEvaluationRuns() {
        if (evaluationRunRepository.count() > 0) {
            LOGGER.info("Skipping demo evaluation seed because evaluation runs already exist");
            return;
        }

        EvaluationScenarioRequest scenario = new EvaluationScenarioRequest();
        scenario.setScenarioName("Demo Evaluation Baseline");
        scenario.setNumNodes(20);
        scenario.setNumEdges(35);
        scenario.setAttackBudget(6);
        scenario.setDefenseBudget(6);
        scenario.setRecoveryBudget(3);
        scenario.setRounds(10);
        scenario.setAlgorithmType(AlgorithmType.CPHS);
        scenario.setEnableMTD(true);
        scenario.setEnableDeception(true);
        scenario.setRepetitions(5);
        scenario.setSeedStrategy(EvaluationSeedStrategy.VARIED);
        scenario.setBaseSeed(42L);

        evaluationFrameworkService.evaluateAndPersist(scenario);
        LOGGER.info("Seeded demo evaluation run");
    }
}
