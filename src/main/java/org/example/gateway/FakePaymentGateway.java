package org.example.gateway;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.UUID;
import java.util.function.DoubleSupplier;

public class FakePaymentGateway implements PaymentGateway {

    private final long simulatedDelayMs;
    private final double failRate;
    private final DoubleSupplier rng;
    private Boolean forceNext = null;

    public FakePaymentGateway(long simulatedDelayMs, double failRate) {
        this(simulatedDelayMs, failRate, new SecureRandom()::nextDouble);
    }

    // Versione deterministica
    public FakePaymentGateway(long simulatedDelayMs, double failRate, long seed) {
        this(simulatedDelayMs, failRate, new SplittableRandom(seed)::nextDouble);
    }

    // Costruttore principale
    private FakePaymentGateway(long simulatedDelayMs, double failRate, DoubleSupplier rng) {
        this.simulatedDelayMs = simulatedDelayMs;
        this.failRate = failRate;
        this.rng = rng;
    }

    public synchronized void forceNext(boolean success) {
        this.forceNext = success;
    }

    @Override
    public PaymentResult charge(int userId, BigDecimal amount, Map<String, String> paymentData)
            throws PaymentGatewayException {
        try {
            Thread.sleep(simulatedDelayMs);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new PaymentGatewayException("Operazione di pagamento interrotta", ie);
        }

        synchronized (this) {
            if (forceNext != null) {
                boolean ok = forceNext;
                forceNext = null;
                return ok
                        ? new PaymentResult(true, "Pagamento forzato OK (sim)", "FAKE-" + UUID.randomUUID(), false)
                        : new PaymentResult(false, "Pagamento forzato FALLITO (sim)", null, false);
            }
        }

        // Validazioni dati carta
        String pan = paymentData.getOrDefault("card_number", "");
        String expiry = paymentData.getOrDefault("expiry", "");
        if (pan.isBlank() || expiry.isBlank()) {
            return new PaymentResult(false, "Dati carta incompleti", null, false);
        }

        // Finta richiesta 3DS se termina con '3'
        String digits = pan.replaceAll("\\D", "");
        if (digits.endsWith("3")) {
            return new PaymentResult(false, "3DS required (sim)", null, true);
        }

        // Fallimento casuale in base a failRate (usa RNG iniettato)
        if (rng.getAsDouble() < failRate) {
            return new PaymentResult(false, "Transazione rifiutata", null, false);
        }

        String txId = "FAKE-" + UUID.randomUUID();
        return new PaymentResult(true, "Pagamento autorizzato", txId, false);
    }
}
