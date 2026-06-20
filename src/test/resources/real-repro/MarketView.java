package cryptobot.web.view.pages;
import cryptobot.service.ExchangeService;

public class MarketView {
    public void newOrder() {
        ExchangeService service = new cryptobot.service.BinanceService();
        placeOrder(service, null, null, null);
    }

    private boolean placeOrder(ExchangeService service, Object orderType, Object price, Object volume) {
        Runnable taskWorker = () -> {
            try {
                service.order(orderType, volume, price);
            } catch (Exception e) {
            }
        };
        taskWorker.run();
        return true;
    }
}
