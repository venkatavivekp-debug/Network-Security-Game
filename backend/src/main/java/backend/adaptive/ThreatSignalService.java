package backend.adaptive;

import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicReference;

@Service
public class ThreatSignalService {

    private final AtomicReference<Double> attackIntensity01 = new AtomicReference<>(0.20);

    public double currentAttackIntensity01() {
        Double v = attackIntensity01.get();
        if (v == null || !Double.isFinite(v)) return 0.20;
        return Math.max(0, Math.min(1, v));
    }

    public void setAttackIntensity01(double value) {
        double v = Double.isFinite(value) ? value : 0.20;
        attackIntensity01.set(Math.max(0, Math.min(1, v)));
    }
}

