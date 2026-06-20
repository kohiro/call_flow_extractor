package mock2;
public class CoinbaseService implements ExchangeService {
    @Override
    public void order(Object type, Object vol, Object price) {
        System.out.println("Coinbase order");
    }
}
