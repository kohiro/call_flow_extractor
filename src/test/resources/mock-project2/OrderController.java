package mock2;

import static mock2.MyUtils.notifySuccess;

public class OrderController {
    public void processOrder() {
        ExchangeService service = new BinanceService();
        placeOrder(service, null, null, null);
    }

    private boolean placeOrder(ExchangeService service, Object orderType, Object price, Object volume) {
        Runnable taskWorker = () -> {
            try {
                service.order(orderType, volume, price);
                notifySuccess("Order placed");
            } catch (Exception e) {
            }
        };
        taskWorker.run();
        return true;
    }
}
