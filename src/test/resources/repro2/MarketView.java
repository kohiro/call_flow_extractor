package cryptobot.web;
import cryptobot.service.ExchangeService;

public class MarketView {
    public void newOrder() {
        placeOrder(new cryptobot.exchange.binance.Binance(), null, null, null);
    }

    private boolean placeOrder(ExchangeService service, Object orderType, Object price, Object volume) {
        service.order(orderType, volume, price);
        return true;
    }
}
