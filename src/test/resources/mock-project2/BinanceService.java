package mock2;
public class BinanceService implements ExchangeService {
    @Override
    public void order(Object type, Object vol, Object price) {
        System.out.println("Binance order");
    }
}
