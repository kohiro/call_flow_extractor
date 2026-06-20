package cryptobot.service;
public abstract class BaseExchangeService implements ExchangeService {
    public void order(Object type, Object vol, Object price) {
        sellSpot(type);
    }
    protected void sellSpot(Object type) { System.out.println("Base sellSpot"); }
}
