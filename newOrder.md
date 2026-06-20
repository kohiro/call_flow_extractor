# Data Flow Extraction

**Start:** `cryptobot.web.view.pages.MarketView#newOrder`

### /Users/kh/git/ccy/cryptobot/src/main/java/cryptobot/web/view/pages/MarketView.java

```java
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.shared.Registration;
import cryptobot.exchange.ExchangeService;
import cryptobot.exchange.ServiceManager;
import cryptobot.strategy.logic.DistributedMarketOrderLogic;
import cryptobot.web.Broadcaster;
import cryptobot.web.entity.Entity.MarketData;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order.OrderType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Predicate;
import static cryptobot.web.view.pages.CommonComponent.*;
import static java.math.BigDecimal.ONE;
import static org.knowm.xchange.dto.Order.OrderType.ASK;
import static org.knowm.xchange.dto.Order.OrderType.BID;

    static final String BTC_PRICE_HTML = """
            <div class="coinmarketcap-currency-widget" data-currencyid="1" data-base="JPY" data-secondary="USD" 
                data-ticker="true" data-rank="false" data-marketcap="true" data-volume="true" data-statsticker="true" data-stats="JPY">
            </div>""";
    @Autowired Broadcaster broadcaster;
    @Autowired DistributedMarketOrderLogic dmo;
    @Autowired ServiceManager serviceManager;
    @Value("${cryptobot.currencyPair}") CurrencyPair ccyp;
    @Value("${cryptobot.price.precision}")  int scale;
    @Value("${cryptobot.volume.precision}") int volScale;
    String volFormat;
    String priceFormat;
    Registration broadcasterRegistration;
    Predicate<? super MarketData> enabled = r->r.getExchangeService().isEnable();
    ExecutorService taskWorker;
    TaskExecutor viewUpdater;
    ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(Thread.ofVirtual().factory());
    private static final String GRID_ID = "market_grid";
    private Grid<MarketData> grid;
    private Text priceDivergence;
    private Dialog dialog;

    private void newOrder(Grid<MarketData> grid, MarketData marketData) {
        if (marketData == null || marketData.getExchangeService().isEnable() == false) {
            notifyError(this.getUI(), "exchange not ebnabled");
            return;
        }
        dialog = new Dialog(){{
            setModality(ModalityMode.MODELESS);
            setDraggable(true);
            setResizable(true);
            setHeaderTitle("New Order");
        }};
        var orderType = new Select<OrderType>("BID/ASK", e->{}, BID, ASK);
        var limitPrice = new NumberField("Limit Price"){{
            setPlaceholder("Limit Price");
            setStep(ONE.movePointLeft(scale - 1).doubleValue());
            setStepButtonsVisible(true);
            setValue(marketData.getBid().doubleValue());
        }};
        var volume = new NumberField("Volume"){{
            setPlaceholder("Volume");
            setStep(ONE.movePointLeft(volScale - 5).doubleValue());
            setStepButtonsVisible(true);
            setValue(Double.valueOf(1));
        }};
        orderType.addValueChangeListener(e->{
            if (BID.equals(e.getValue())) {
                limitPrice.setValue(marketData.getBid().doubleValue());
            } else {
                limitPrice.setValue(marketData.getAsk().doubleValue());
            }
        });
        dialog.add(new VerticalLayout(orderType, limitPrice, volume){{
            setPadding(false);
            setSpacing(false);
        }});
        var cancel = new Button("Cancel", e -> dialog.close());
        var execute = new Button("Execute", e ->{
            boolean success = placeOrder(
                marketData.getExchangeService(), orderType.getValue(),
                new BigDecimal(limitPrice.getValue()), new BigDecimal(volume.getValue()));
            if (success) dialog.close();
        });
        execute.addThemeVariants(ButtonVariant.LUMO_ERROR);
        dialog.getFooter().add(cancel, execute);
        dialog.open();
    }

```
### /Users/kh/git/ccy/cryptobot/src/main/java/cryptobot/web/view/pages/MarketView.java

```java
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.shared.Registration;
import cryptobot.exchange.ExchangeService;
import cryptobot.exchange.ServiceManager;
import cryptobot.strategy.logic.DistributedMarketOrderLogic;
import cryptobot.web.Broadcaster;
import cryptobot.web.entity.Entity.MarketData;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order.OrderType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import java.math.BigDecimal;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Predicate;
import static cryptobot.web.view.pages.CommonComponent.*;
import static java.lang.String.format;

    static final String BTC_PRICE_HTML = """
            <div class="coinmarketcap-currency-widget" data-currencyid="1" data-base="JPY" data-secondary="USD" 
                data-ticker="true" data-rank="false" data-marketcap="true" data-volume="true" data-statsticker="true" data-stats="JPY">
            </div>""";
    @Autowired Broadcaster broadcaster;
    @Autowired DistributedMarketOrderLogic dmo;
    @Autowired ServiceManager serviceManager;
    @Value("${cryptobot.currencyPair}") CurrencyPair ccyp;
    @Value("${cryptobot.price.precision}")  int scale;
    @Value("${cryptobot.volume.precision}") int volScale;
    String volFormat;
    String priceFormat;
    Registration broadcasterRegistration;
    Predicate<? super MarketData> enabled = r->r.getExchangeService().isEnable();
    ExecutorService taskWorker;
    TaskExecutor viewUpdater;
    ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(Thread.ofVirtual().factory());
    private static final String GRID_ID = "market_grid";
    private Grid<MarketData> grid;
    private Text priceDivergence;
    private Dialog dialog;

    private boolean placeOrder(ExchangeService service, OrderType orderType, BigDecimal price, BigDecimal volume) {
        if (orderType == null) {
            notifyError(this.getUI(),"Order Type in not selected");
            return false;
        }
        if (price == null) {
            notifyError(this.getUI(),"price not specified");
            return false;
        }
        if (volume == null) {
            notifyError(this.getUI(),"Volume not specified");
            return false;
        }
        taskWorker.execute(()->{
            try {
                service.order(orderType, volume, price);
                notifySuccess(this.getUI(),"Order placed");
            } catch (Exception e) {
                notifyError(this.getUI(),"Order error\n%s".formatted(e.getMessage()));
            }
        });
        return true;
    }

```
### /Users/kh/git/ccy/cryptobot/src/main/java/cryptobot/exchange/ExchangeService.java

```java
import static java.math.BigDecimal.*;
import static java.math.RoundingMode.*;
import static java.util.Optional.*;
import static java.util.stream.Collectors.*;
import static org.knowm.xchange.dto.Order.OrderStatus.*;
import static org.knowm.xchange.dto.Order.OrderType.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import cryptobot.dto.*;
import org.springframework.web.client.RestClient;
import org.knowm.xchange.BaseExchange;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.Order.OrderType;
import org.knowm.xchange.dto.account.Balance;
import org.knowm.xchange.dto.account.Fee;
import org.knowm.xchange.dto.marketdata.Ticker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import cryptobot.AppConfig;
import cryptobot.misc.MailSender;

	@Value("${cryptobot.currencyPair}") protected CurrencyPair ccyp;
	@Value("${cryptobot.price.precision}") protected int scale;
	@Value("${cryptobot.volume.precision}") protected int volScale;
	protected BaseExchange xchange;
	protected final ConcurrentHashMap<Currency, BalanceInfo> balanceInfoMap = new ConcurrentHashMap<>();
	protected final OrderPool orderPool = new OrderPool();
	protected volatile Ticker lastTick;
	protected final Logger log = LoggerFactory.getLogger(this.getClass());
	private boolean enable = true;
	private boolean allowBid = true;
	private boolean allowAsk = true;
	@Autowired ApplicationEventPublisher publisher;
	@Autowired protected AppConfig config;
	@Autowired protected RestClient restClient;
	@Autowired MailSender mailer;
	protected Fee fee = new Fee(ZERO, ZERO);
	public static enum FeeType
	@Autowired TickerLogger tickerLogger;
		final Set<String> expectedOrderId = new HashSet<>();

	/**
	 * 成行注文
	 * @param type 注文の方向
	 * @param volume 数量
	 * @return 注文送信後に注文情報を取得した結果の注文レコード
	 * @throws Exception
	 */
	public OrderRecord order(OrderType type, BigDecimal volume) throws Exception {
		var bookType = type.getOpposite();
		getOrderbookService().takeOrder(bookType, volume);
		OrderRecord order;
        try {
            if (type.equals(OrderType.BID)) {
                order = buySpot(volume.setScale(volScale, HALF_UP));
            } else {
                order = sellSpot(volume.setScale(volScale, HALF_UP));
            }
			log.info("market order created. order: {}", order);
			var price = order.getAveragePrice();
            getOrderbookService().correctBestPrice(bookType, price);
            return order;
        } catch (Exception e) {
			mailer.info("exchange disabled on order error", "disabled exchange: " + getName());
			setEnable(false);
			throw e;
        }
    }

```
### /Users/kh/git/ccy/cryptobot/src/main/java/cryptobot/exchange/ExchangeService.java

```java
import static java.math.BigDecimal.*;
import static java.math.RoundingMode.*;
import static java.util.Optional.*;
import static java.util.stream.Collectors.*;
import static org.knowm.xchange.dto.Order.OrderStatus.*;
import static org.knowm.xchange.dto.Order.OrderType.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import cryptobot.dto.*;
import org.springframework.web.client.RestClient;
import org.knowm.xchange.BaseExchange;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.Order.OrderType;
import org.knowm.xchange.dto.account.Balance;
import org.knowm.xchange.dto.account.Fee;
import org.knowm.xchange.dto.marketdata.Ticker;
import org.knowm.xchange.dto.marketdata.Trade;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.knowm.xchange.service.trade.TradeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import cryptobot.AppConfig;
import cryptobot.misc.MailSender;

	@Value("${cryptobot.currencyPair}") protected CurrencyPair ccyp;
	@Value("${cryptobot.price.precision}") protected int scale;
	@Value("${cryptobot.volume.precision}") protected int volScale;
	protected BaseExchange xchange;
	protected final ConcurrentHashMap<Currency, BalanceInfo> balanceInfoMap = new ConcurrentHashMap<>();
	protected final OrderPool orderPool = new OrderPool();
	protected volatile Ticker lastTick;
	protected final Logger log = LoggerFactory.getLogger(this.getClass());
	private boolean enable = true;
	private boolean allowBid = true;
	private boolean allowAsk = true;
	@Autowired ApplicationEventPublisher publisher;
	@Autowired protected AppConfig config;
	@Autowired protected RestClient restClient;
	@Autowired MailSender mailer;
	protected Fee fee = new Fee(ZERO, ZERO);
	public static enum FeeType
	@Autowired TickerLogger tickerLogger;
		final Set<String> expectedOrderId = new HashSet<>();

	public OrderRecord buySpot(BigDecimal volume, BigDecimal limitPrice) throws Exception {
		LimitOrder order = new LimitOrder(OrderType.BID, volume, ccyp, null, null, limitPrice);
		String id = xchange.getTradeService().placeLimitOrder(order);
		return getOrder(id);
	}

```
### /Users/kh/git/ccy/cryptobot/src/main/java/cryptobot/exchange/ExchangeService.java

```java
import static java.math.BigDecimal.*;
import static java.math.RoundingMode.*;
import static java.util.Optional.*;
import static java.util.stream.Collectors.*;
import static org.knowm.xchange.dto.Order.OrderStatus.*;
import static org.knowm.xchange.dto.Order.OrderType.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import cryptobot.dto.*;
import org.springframework.web.client.RestClient;
import org.knowm.xchange.BaseExchange;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.account.Balance;
import org.knowm.xchange.dto.account.Fee;
import org.knowm.xchange.dto.marketdata.Ticker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import cryptobot.AppConfig;
import cryptobot.misc.MailSender;

	@Value("${cryptobot.currencyPair}") protected CurrencyPair ccyp;
	@Value("${cryptobot.price.precision}") protected int scale;
	@Value("${cryptobot.volume.precision}") protected int volScale;
	protected BaseExchange xchange;
	protected final ConcurrentHashMap<Currency, BalanceInfo> balanceInfoMap = new ConcurrentHashMap<>();
	protected final OrderPool orderPool = new OrderPool();
	protected volatile Ticker lastTick;
	protected final Logger log = LoggerFactory.getLogger(this.getClass());
	private boolean enable = true;
	private boolean allowBid = true;
	private boolean allowAsk = true;
	@Autowired ApplicationEventPublisher publisher;
	@Autowired protected AppConfig config;
	@Autowired protected RestClient restClient;
	@Autowired MailSender mailer;
	protected Fee fee = new Fee(ZERO, ZERO);
	public static enum FeeType
	@Autowired TickerLogger tickerLogger;
		final Set<String> expectedOrderId = new HashSet<>();

	abstract public OrderRecord getOrder(String id) throws Exception;

```
### /Users/kh/git/ccy/cryptobot/src/main/java/cryptobot/exchange/binance/Binance.java

```java
import cryptobot.dto.OrderRecord;
import cryptobot.exchange.OrderbookService;
import org.knowm.xchange.binance.service.BinanceAccountService;
import org.knowm.xchange.binance.service.BinanceTradeService;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.knowm.xchange.dto.trade.LimitOrder.Builder;
import org.knowm.xchange.instrument.Instrument;
import org.knowm.xchange.service.trade.TradeService;
import org.knowm.xchange.service.trade.params.orders.OrderQueryParamInstrument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.*;

	@Autowired BinanceOrderbookService orderbookService;
	@Value("${binance.api.key}") private String API_KEY;
	@Value("${binance.api.secret}") private String SECRET;
	@Value("${binance.fee.withdraw.counter}") BigDecimal COUNTER_WITHDRAW_FEE;
	@Value("${binance.fee.withdraw.base}") BigDecimal BASE_WITHDRAW_FEE;
	@Value("${binance.address.eth:#{null}}") String ETH_ADDRESS;
	@Value("${binance.address.btc:#{null}}") String BTC_ADDRESS;
	@Value("${binance.address.bch:#{null}}") String BCH_ADDRESS;
	@Value("${binance.address.xlm:#{null}}") String XLM_ADDRESS;
	@Value("${binance.address.xlm.memo:#{null}}") String XLM_MEMO;
	@Value("${binance.rebalance.threshold.base:0}") BigDecimal rebalanceThresholdBase;
	@Value("${binance.rebalance.target.base:0}") BigDecimal rebalanceTargetBase;
	@Value("${binance.rebalance.threshold.counter:0}") BigDecimal rebalanceThresholdCounter;
	@Value("${binance.rebalance.target.counter:0}") BigDecimal rebalanceTargetCounter;
	BinanceTradeService tradeService;
	BinanceAccountService accountService;
	private final int stepSize = 4;
	@Value("${exchange.api.binance:#{null}}") private String API_ENDPOINT;

	synchronized public OrderRecord getOrder(String orderId) throws Exception {
		var present = orderPool.getById(orderId);
		if (present != null) {
			return present;
		}
		OrderQueryParamInstrument param = new OrderQueryParamInstrument() {
			@Override public String getOrderId() {return orderId;}
			@Override public void setOrderId(String id) {}
			@Override public Instrument getInstrument() {return ccyp;}
			@Override public void setInstrument(Instrument instrument) {}
		};
		Collection<Order> orderList = tradeService.getOrder(param);
		if (orderList.isEmpty()) {
			return null;
		}
		Order order = orderList.iterator().next();
		LimitOrder limitOrder = Builder.from(order).build();
		OrderRecord record = new OrderRecord(this, limitOrder);
		BigDecimal executedVol = order.getCumulativeAmount();
		// Builder.from(order)でコピーされないため個別コピー(最新のxchangeライブラリでは解消？)
		record.setCumulativeAmount(order.getCumulativeAmount());
		return record;
	}

```
### /Users/kh/git/ccy/cryptobot/src/main/java/cryptobot/exchange/bitbank/Bitbank.java

```java
import static cryptobot.exchange.ExchangeService.FeeType.*;
import static java.lang.String.*;
import static java.math.BigDecimal.*;
import static java.math.RoundingMode.*;
import static java.util.Objects.*;
import static java.util.stream.Collectors.*;
import static org.apache.commons.lang3.StringUtils.*;
import static org.knowm.xchange.dto.Order.OrderStatus.*;
import static org.knowm.xchange.dto.Order.OrderType.*;
import java.math.BigDecimal;
import java.util.Map;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import cryptobot.dto.OrderRecord;
import cryptobot.exchange.OrderbookService;
import cryptobot.exchange.PlacedOrderNotFoundException;
import cryptobot.exchange.bitbank.dto.*;
import cryptobot.misc.MailSender;

	@Value("${exchange.api.bitbank:https://api.bitbank.cc}")
	private String API_ENDPOINT;
	@Value("${bitbank.web.2fa.secret}") String SECRET_KEY_FOR_2FA;
	@Value("${bitbank.api.key}") String API_KEY;
	@Value("${bitbank.api.secret}") String SECRET;
	@Value("${bitbank.fee.withdraw.counter}") BigDecimal COUNTER_WITHDRAW_FEE;
	@Value("${bitbank.fee.withdraw.base}") BigDecimal BASE_WITHDRAW_FEE;
	@Value("${bitbank.address.eth:#{null}}") String ETH_ADDRESS;
	@Value("${bitbank.address.bch:#{null}}") String BCH_ADDRESS;
	@Value("${bitbank.address.btc:#{null}}") String BTC_ADDRESS;
	@Value("${bitbank.rebalance.threshold.base:0}") BigDecimal rebalanceThresholdBase;
	@Value("${bitbank.rebalance.target.base:0}") BigDecimal rebalanceTargetBase;
	@Value("${bitbank.rebalance.threshold.counter:0}") BigDecimal rebalanceThresholdCounter;
	@Value("${bitbank.rebalance.target.counter:0}") BigDecimal rebalanceTargetCounter;
	@Value("${cryptobot.healthcheck}") private String enableHealthCheck;
	@Autowired MailSender mail;
	@Autowired BitbankOrderbookService orderbookService;
	BitbankAssets assetsResponse = null;
	String ccyps;
	private Map<String, String> errorCodes;
	static Map<CurrencyPair, String> SUPPORTED_CCYPS = Map.of(
			CurrencyPair.BTC_JPY, "BTC_JPY"
	);

	@Override
	synchronized public OrderRecord getOrder(String id) throws Exception {
		var path = "/v1/user/spot/order";
		var order = doHttpGet(path, BitbankOrders.Order.class, Map.of("pair", ccyps, "order_id", id));
		if (order == null || order.orderId == 0)
			throw new PlacedOrderNotFoundException("order not exists");
		OrderRecord record = new OrderRecord(this, order.converLimitOrder());
		// 指値注文の場合(＝Priceが０でない)
		if (nonNull(record.getLimitPrice()) && record.getLimitPrice().compareTo(ZERO) != 0) {
			// 指値の場合はaveragePriceが取得出来ないのでpriceをaveragePriceとして設定
			record.setAveragePrice(record.getLimitPrice());
		}
		return record;
	}

```
### /Users/kh/git/ccy/cryptobot/src/main/java/cryptobot/exchange/bitbank/Bitbank.java

```java
import static cryptobot.exchange.ExchangeService.FeeType.*;
import static java.lang.String.*;
import static java.math.BigDecimal.*;
import static java.math.RoundingMode.*;
import static java.util.Objects.*;
import static java.util.stream.Collectors.*;
import static org.apache.commons.lang3.StringUtils.*;
import static org.knowm.xchange.dto.Order.OrderStatus.*;
import static org.knowm.xchange.dto.Order.OrderType.*;
import static org.springframework.web.util.UriComponentsBuilder.fromUriString;
import java.math.BigDecimal;
import java.util.Map;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.JsonNode;
import cryptobot.exchange.OrderbookService;
import cryptobot.exchange.bitbank.dto.*;
import cryptobot.misc.JsonUtils;
import cryptobot.misc.MailSender;
import org.springframework.web.client.RestClient.ResponseSpec.ErrorHandler;

	@Value("${exchange.api.bitbank:https://api.bitbank.cc}")
	private String API_ENDPOINT;
	@Value("${bitbank.web.2fa.secret}") String SECRET_KEY_FOR_2FA;
	@Value("${bitbank.api.key}") String API_KEY;
	@Value("${bitbank.api.secret}") String SECRET;
	@Value("${bitbank.fee.withdraw.counter}") BigDecimal COUNTER_WITHDRAW_FEE;
	@Value("${bitbank.fee.withdraw.base}") BigDecimal BASE_WITHDRAW_FEE;
	@Value("${bitbank.address.eth:#{null}}") String ETH_ADDRESS;
	@Value("${bitbank.address.bch:#{null}}") String BCH_ADDRESS;
	@Value("${bitbank.address.btc:#{null}}") String BTC_ADDRESS;
	@Value("${bitbank.rebalance.threshold.base:0}") BigDecimal rebalanceThresholdBase;
	@Value("${bitbank.rebalance.target.base:0}") BigDecimal rebalanceTargetBase;
	@Value("${bitbank.rebalance.threshold.counter:0}") BigDecimal rebalanceThresholdCounter;
	@Value("${bitbank.rebalance.target.counter:0}") BigDecimal rebalanceTargetCounter;
	@Value("${cryptobot.healthcheck}") private String enableHealthCheck;
	@Autowired MailSender mail;
	@Autowired BitbankOrderbookService orderbookService;
	BitbankAssets assetsResponse = null;
	String ccyps;
	private Map<String, String> errorCodes;
	static Map<CurrencyPair, String> SUPPORTED_CCYPS = Map.of(
			CurrencyPair.BTC_JPY, "BTC_JPY"
	);

	protected <T> T doHttpGet(String path, Class<T> clazz, Map<String, String> params) throws Exception {
		Map<String, String> query = params != null ? params : Map.of();
		Map<String, String> headers = getPrivateRequestHeader(path, query);
		
		var uriBuilder = fromUriString(API_ENDPOINT + path);
		query.forEach(uriBuilder::queryParam);

		ErrorHandler errorHandler = (req, res) -> {
			throw new RuntimeException("Bitbank API error: " + res.getStatusCode());
		};
		String res = restClient.get()
				.uri(uriBuilder.build().toUri())
				.headers(h -> headers.forEach(h::add))
				.retrieve()
				.onStatus(s -> s.isError(), errorHandler)
				.body(String.class);
		JsonNode data = JsonUtils.MAPPER.readTree(res).path("data");
		return data.traverse(JsonUtils.MAPPER).readValueAs(clazz);

	}

```
### /Users/kh/git/ccy/cryptobot/src/main/java/cryptobot/exchange/bitbank/Bitbank.java

```java
import static cryptobot.exchange.ExchangeService.FeeType.*;
import static java.lang.String.*;
import static java.lang.System.currentTimeMillis;
import static java.math.BigDecimal.*;
import static java.math.RoundingMode.*;
import static java.util.Objects.*;
import static java.util.stream.Collectors.*;
import static org.apache.commons.lang3.StringUtils.*;
import static org.knowm.xchange.dto.Order.OrderStatus.*;
import static org.knowm.xchange.dto.Order.OrderType.*;
import java.math.BigDecimal;
import java.util.Map;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import cryptobot.exchange.OrderbookService;
import cryptobot.exchange.bitbank.dto.*;
import cryptobot.misc.MailSender;

	@Value("${exchange.api.bitbank:https://api.bitbank.cc}")
	private String API_ENDPOINT;
	@Value("${bitbank.web.2fa.secret}") String SECRET_KEY_FOR_2FA;
	@Value("${bitbank.api.key}") String API_KEY;
	@Value("${bitbank.api.secret}") String SECRET;
	@Value("${bitbank.fee.withdraw.counter}") BigDecimal COUNTER_WITHDRAW_FEE;
	@Value("${bitbank.fee.withdraw.base}") BigDecimal BASE_WITHDRAW_FEE;
	@Value("${bitbank.address.eth:#{null}}") String ETH_ADDRESS;
	@Value("${bitbank.address.bch:#{null}}") String BCH_ADDRESS;
	@Value("${bitbank.address.btc:#{null}}") String BTC_ADDRESS;
	@Value("${bitbank.rebalance.threshold.base:0}") BigDecimal rebalanceThresholdBase;
	@Value("${bitbank.rebalance.target.base:0}") BigDecimal rebalanceTargetBase;
	@Value("${bitbank.rebalance.threshold.counter:0}") BigDecimal rebalanceThresholdCounter;
	@Value("${bitbank.rebalance.target.counter:0}") BigDecimal rebalanceTargetCounter;
	@Value("${cryptobot.healthcheck}") private String enableHealthCheck;
	@Autowired MailSender mail;
	@Autowired BitbankOrderbookService orderbookService;
	BitbankAssets assetsResponse = null;
	String ccyps;
	private Map<String, String> errorCodes;
	static Map<CurrencyPair, String> SUPPORTED_CCYPS = Map.of(
			CurrencyPair.BTC_JPY, "BTC_JPY"
	);

	protected Map<String, String> getPrivateRequestHeader(String json) {
		long nonce = currentTimeMillis();
		String message = String.valueOf(nonce) + json;
		return makePrivateRequestHeaders(nonce, createHMAC(SECRET, message));
	}

```
### /Users/kh/git/ccy/cryptobot/src/main/java/cryptobot/exchange/bitbank/Bitbank.java

```java
import static cryptobot.exchange.ExchangeService.FeeType.*;
import static java.lang.String.*;
import static java.math.BigDecimal.*;
import static java.math.RoundingMode.*;
import static java.util.Objects.*;
import static java.util.stream.Collectors.*;
import static org.apache.commons.lang3.StringUtils.*;
import static org.knowm.xchange.dto.Order.OrderStatus.*;
import static org.knowm.xchange.dto.Order.OrderType.*;
import java.math.BigDecimal;
import java.util.Map;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import cryptobot.exchange.OrderbookService;
import cryptobot.exchange.bitbank.dto.*;
import cryptobot.misc.MailSender;

	@Value("${exchange.api.bitbank:https://api.bitbank.cc}")
	private String API_ENDPOINT;
	@Value("${bitbank.web.2fa.secret}") String SECRET_KEY_FOR_2FA;
	@Value("${bitbank.api.key}") String API_KEY;
	@Value("${bitbank.api.secret}") String SECRET;
	@Value("${bitbank.fee.withdraw.counter}") BigDecimal COUNTER_WITHDRAW_FEE;
	@Value("${bitbank.fee.withdraw.base}") BigDecimal BASE_WITHDRAW_FEE;
	@Value("${bitbank.address.eth:#{null}}") String ETH_ADDRESS;
	@Value("${bitbank.address.bch:#{null}}") String BCH_ADDRESS;
	@Value("${bitbank.address.btc:#{null}}") String BTC_ADDRESS;
	@Value("${bitbank.rebalance.threshold.base:0}") BigDecimal rebalanceThresholdBase;
	@Value("${bitbank.rebalance.target.base:0}") BigDecimal rebalanceTargetBase;
	@Value("${bitbank.rebalance.threshold.counter:0}") BigDecimal rebalanceThresholdCounter;
	@Value("${bitbank.rebalance.target.counter:0}") BigDecimal rebalanceTargetCounter;
	@Value("${cryptobot.healthcheck}") private String enableHealthCheck;
	@Autowired MailSender mail;
	@Autowired BitbankOrderbookService orderbookService;
	BitbankAssets assetsResponse = null;
	String ccyps;
	private Map<String, String> errorCodes;
	static Map<CurrencyPair, String> SUPPORTED_CCYPS = Map.of(
			CurrencyPair.BTC_JPY, "BTC_JPY"
	);

	private Map<String, String> makePrivateRequestHeaders(long nonce, String sign) {
		return Map.of(
				"Content-Type", "application/json; charset=utf-8",
				"ACCESS-KEY", API_KEY,
				"ACCESS-NONCE", String.valueOf(nonce),
				"ACCESS-SIGNATURE", sign);
	}

```
### /Users/kh/git/ccy/cryptobot/src/main/java/cryptobot/exchange/GenericService.java

```java
import static java.math.BigDecimal.*;
import static org.knowm.xchange.dto.Order.OrderStatus.*;
import org.apache.commons.codec.binary.Hex;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

	protected final Logger log = LoggerFactory.getLogger(getClass());
	protected volatile boolean serviceStarted = false;

    @NotNull
    protected String createHMAC(String secret, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(secret.getBytes(), "HmacSHA256");
            mac.init(secret_key);
            mac.update(data.getBytes());
            char[] hash = Hex.encodeHex(mac.doFinal());
            return new String(hash);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

```
### /Users/kh/git/ccy/cryptobot/src/main/java/cryptobot/exchange/coincheck/Coincheck.java

```java
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import cryptobot.dto.OrderRecord;
import cryptobot.exchange.OrderbookService;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.*;
import static org.knowm.xchange.currency.CurrencyPair.BTC_JPY;
import static org.knowm.xchange.dto.Order.OrderStatus.*;

    @Value("${coincheck.api.key}") private String API_KEY;
    @Value("${coincheck.api.secret}") private String SECRET;
    @Autowired CoincheckOrderbookService orderbookService;
    static Map<CurrencyPair, String> SUPPORTED_CCYPS = Map.of(BTC_JPY, "BTC_JPY");
        /** id 注文のID（新規注文でのIDと同一です）*/
        @JsonProperty("id")  String orderId;
        /** rate 注文のレート（ null の場合は成り行き注文です）*/
        @JsonProperty("rate")  BigDecimal limitPrice;
        /** pending_amount 注文の未決済の量 */
        @JsonProperty("pending_amount")  BigDecimal outstandingVolume;
        /** pending_market_buy_amount 注文の未決済の量（現物成行買いの場合のみ） */
        @JsonProperty("pending_market_buy_amount")  BigDecimal outstandingMarketVolume;
        /** order_type 注文のタイプ（"sell" or "buy"）*/
        @JsonProperty("order_type")  String orderType;
        /** stop_loss_rate 逆指値レート */
        @JsonProperty("stop_loss_rate")  BigDecimal stop_loss_rate;
        /** pair 取引ペア */
        @JsonProperty("pair")  CurrencyPair currencyPair;
        /** created_at 注文の作成日時 */
        @JsonProperty("created_at")  String timestamp;

    @Override
    public OrderRecord getOrder(String id) throws Exception {
        List<LimitOrder> openOrders =  getOpenOrders();
        var order = openOrders.stream().filter(o->o.getId().equals(id)).findAny();
        if (order.isPresent()){
            return new OrderRecord(this, order.get());
        }
        var op = orderPool.getById(id);
        if (op != null) {
            var limitOrder = op;
            var result = sendRequest("/api/exchange/orders/cancel_status?id=%s".formatted(id), JsonNode.class);
            if (result.path("success").asBoolean()) {
                if (result.path("cancel").asBoolean()) {
                    limitOrder.setOrderStatus(CANCELED);
                } else {
                    // open orderになしでcancel状態でない＝filledと想定
                    limitOrder.setOrderStatus(FILLED);
                }
            } else {
                // order状態が取引システムに未反映のため取得に失敗したと想定→状態変化なし
            }
            return new OrderRecord(this, limitOrder);
        }
        return null;
    }

```
### /Users/kh/git/ccy/cryptobot/src/main/java/cryptobot/exchange/coincheck/Coincheck.java

```java
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import cryptobot.exchange.OrderbookService;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order.OrderType;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import static java.math.BigDecimal.ZERO;
import static org.knowm.xchange.coincheck.CoincheckAdapter.createOrderType;
import static org.knowm.xchange.currency.CurrencyPair.BTC_JPY;
import static org.knowm.xchange.dto.Order.OrderStatus.*;

    @Value("${coincheck.api.key}") private String API_KEY;
    @Value("${coincheck.api.secret}") private String SECRET;
    @Autowired CoincheckOrderbookService orderbookService;
    static Map<CurrencyPair, String> SUPPORTED_CCYPS = Map.of(BTC_JPY, "BTC_JPY");
        /** id 注文のID（新規注文でのIDと同一です）*/
        @JsonProperty("id")  String orderId;
        /** rate 注文のレート（ null の場合は成り行き注文です）*/
        @JsonProperty("rate")  BigDecimal limitPrice;
        /** pending_amount 注文の未決済の量 */
        @JsonProperty("pending_amount")  BigDecimal outstandingVolume;
        /** pending_market_buy_amount 注文の未決済の量（現物成行買いの場合のみ） */
        @JsonProperty("pending_market_buy_amount")  BigDecimal outstandingMarketVolume;
        /** order_type 注文のタイプ（"sell" or "buy"）*/
        @JsonProperty("order_type")  String orderType;
        /** stop_loss_rate 逆指値レート */
        @JsonProperty("stop_loss_rate")  BigDecimal stop_loss_rate;
        /** pair 取引ペア */
        @JsonProperty("pair")  CurrencyPair currencyPair;
        /** created_at 注文の作成日時 */
        @JsonProperty("created_at")  String timestamp;

    @Override
    public List<LimitOrder> getOpenOrders() throws IOException {
        var path = "/api/exchange/orders/opens";
        JsonNode json = sendRequest(path, JsonNode.class);
        if (!json.path("success").asBoolean()) {
            throw new RuntimeException("get order list failed.");
        }
        var jsonarray = json.path("orders");
        List<LimitOrder> limitOrders = new ArrayList<>();
        for (JsonNode o : jsonarray) {
            var pool = orderPool.getById(o.get("id").asText());
            OrderType orderType = createOrderType(o.get("order_type").asText());
            CurrencyPair cp = new CurrencyPair(o.get("pair").asText().replace("_", "/"));
            if (!cp.equals(ccyp)) continue;

            var limitOrder = new LimitOrder.Builder(orderType, cp)
                .id(o.get("id").asText())
                .orderType(createOrderType(o.get("order_type").asText()))
                .originalAmount(pool != null ? pool.getOriginalAmount() : null)
                .remainingAmount(o.path("pending_amount").isNull() ?
                    (o.path("pending_market_buy_amount").isNull() ? ZERO : new BigDecimal(o.path("pending_market_buy_amount").asText()))
                    :new BigDecimal(o.path("pending_amount").asText()))
                .timestamp(parseDatetime(o.get("created_at").asText()))
                .limitPrice(o.path("rate").isNull() ? null:
                        new BigDecimal(o.path("rate").asText()))
                .build();
            if (limitOrder.getOriginalAmount() == null) {
                limitOrder.setOrderStatus(NEW);
            } else if (ZERO.compareTo(limitOrder.getRemainingAmount()) == 0) {
                limitOrder.setOrderStatus(FILLED);
            } else if (ZERO.compareTo(limitOrder.getCumulativeAmount()) < 0) {
                limitOrder.setOrderStatus(PARTIALLY_FILLED);
            } else {
                limitOrder.setOrderStatus(OPEN);
            }
            limitOrders.add(limitOrder);
        }
        return limitOrders;
    }

```
### /Users/kh/git/ccy/cryptobot/src/main/java/cryptobot/exchange/coincheck/Coincheck.java

```java
import com.fasterxml.jackson.annotation.JsonProperty;
import cryptobot.exchange.OrderbookService;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.*;
import static org.knowm.xchange.currency.CurrencyPair.BTC_JPY;
import static org.knowm.xchange.dto.Order.OrderStatus.*;

    @Value("${coincheck.api.key}") private String API_KEY;
    @Value("${coincheck.api.secret}") private String SECRET;
    @Autowired CoincheckOrderbookService orderbookService;
    static Map<CurrencyPair, String> SUPPORTED_CCYPS = Map.of(BTC_JPY, "BTC_JPY");
        /** id 注文のID（新規注文でのIDと同一です）*/
        @JsonProperty("id")  String orderId;
        /** rate 注文のレート（ null の場合は成り行き注文です）*/
        @JsonProperty("rate")  BigDecimal limitPrice;
        /** pending_amount 注文の未決済の量 */
        @JsonProperty("pending_amount")  BigDecimal outstandingVolume;
        /** pending_market_buy_amount 注文の未決済の量（現物成行買いの場合のみ） */
        @JsonProperty("pending_market_buy_amount")  BigDecimal outstandingMarketVolume;
        /** order_type 注文のタイプ（"sell" or "buy"）*/
        @JsonProperty("order_type")  String orderType;
        /** stop_loss_rate 逆指値レート */
        @JsonProperty("stop_loss_rate")  BigDecimal stop_loss_rate;
        /** pair 取引ペア */
        @JsonProperty("pair")  CurrencyPair currencyPair;
        /** created_at 注文の作成日時 */
        @JsonProperty("created_at")  String timestamp;

    private <T> T sendRequest(String path, Class<T> responseClass) {
        var url = "https://coincheck.com" + path;
        var nonce = String.valueOf(new Date().getTime());
        var signature = createHMAC(SECRET, nonce + url);

        return restClient.get()
                .uri(url)
                .header("ACCESS-KEY", API_KEY)
                .header("ACCESS-NONCE", nonce)
                .header("ACCESS-SIGNATURE", signature)
                .retrieve()
                .body(responseClass);
    }

```
### /Users/kh/git/ccy/cryptobot/src/main/java/cryptobot/exchange/coincheck/Coincheck.java

```java
import com.fasterxml.jackson.annotation.JsonProperty;
import cryptobot.exchange.OrderbookService;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.text.ParseException;
import java.util.*;
import static org.knowm.xchange.currency.CurrencyPair.BTC_JPY;
import static org.knowm.xchange.dto.Order.OrderStatus.*;

    @Value("${coincheck.api.key}") private String API_KEY;
    @Value("${coincheck.api.secret}") private String SECRET;
    @Autowired CoincheckOrderbookService orderbookService;
    static Map<CurrencyPair, String> SUPPORTED_CCYPS = Map.of(BTC_JPY, "BTC_JPY");
        /** id 注文のID（新規注文でのIDと同一です）*/
        @JsonProperty("id")  String orderId;
        /** rate 注文のレート（ null の場合は成り行き注文です）*/
        @JsonProperty("rate")  BigDecimal limitPrice;
        /** pending_amount 注文の未決済の量 */
        @JsonProperty("pending_amount")  BigDecimal outstandingVolume;
        /** pending_market_buy_amount 注文の未決済の量（現物成行買いの場合のみ） */
        @JsonProperty("pending_market_buy_amount")  BigDecimal outstandingMarketVolume;
        /** order_type 注文のタイプ（"sell" or "buy"）*/
        @JsonProperty("order_type")  String orderType;
        /** stop_loss_rate 逆指値レート */
        @JsonProperty("stop_loss_rate")  BigDecimal stop_loss_rate;
        /** pair 取引ペア */
        @JsonProperty("pair")  CurrencyPair currencyPair;
        /** created_at 注文の作成日時 */
        @JsonProperty("created_at")  String timestamp;

    private static Date parseDatetime(String datetime) {
        try {
            return DateFormatUtils.ISO_8601_EXTENDED_DATETIME_FORMAT
                    .parse(datetime);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

```
### /Users/kh/git/ccy/cryptobot/src/main/java/cryptobot/exchange/bitfinex/Bitfinex.java

```java
import static cryptobot.exchange.ExchangeService.FeeType.*;
import static java.math.BigDecimal.*;
import static java.math.RoundingMode.*;
import static java.util.Objects.*;
import static java.util.stream.Collectors.*;
import static org.knowm.xchange.dto.Order.OrderStatus.*;
import static org.knowm.xchange.dto.Order.OrderType.*;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import org.knowm.xchange.bitfinex.service.BitfinexAdapters;
import org.knowm.xchange.bitfinex.service.BitfinexTradeService;
import org.knowm.xchange.bitfinex.v1.BitfinexUtils;
import org.knowm.xchange.bitfinex.v1.dto.trade.BitfinexOrderStatusResponse;
import org.knowm.xchange.dto.Order.OrderStatus;
import org.knowm.xchange.dto.Order.OrderType;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import cryptobot.dto.OrderRecord;
import cryptobot.exchange.OrderbookService;

	private static final BigDecimal TRADING_CHARGE = new BigDecimal("0.00200000");
	private BitfinexTradeService tradeService;
	@Value("${bitfinex.api.key}") private String API_KEY;
	@Value("${bitfinex.api.secret}") private String SECRET;
	@Value("${bitfinex.fee.withdraw.counter}") BigDecimal COUNTER_WITHDRAW_FEE;
	@Value("${bitfinex.fee.withdraw.base}") BigDecimal BASE_WITHDRAW_FEE;
	@Value("${bitfinex.address.bch:#{null}}") String BCH_ADDRESS;
	@Value("${bitfinex.address.xlm:#{null}}") String XLM_ADDRESS;
	@Value("${bitfinex.address.xlm.memo:#{null}}") String XLM_MEMO;
	@Value("${bitfinex.rebalance.threshold.base:0}") BigDecimal rebalanceThresholdBase;
	@Value("${bitfinex.rebalance.target.base:0}") BigDecimal rebalanceTargetBase;
	@Value("${bitfinex.rebalance.threshold.counter:0}") BigDecimal rebalanceThresholdCounter;
	@Value("${bitfinex.rebalance.target.counter:0}") BigDecimal rebalanceTargetCounter;
	@Autowired BitfinexOrderbookService orderbookService;

	@Override
	synchronized public OrderRecord getOrder(String id) throws IOException {
		// 注文直後だと「BitfinexExceptionV1: No such order found」となる。
//		BitfinexOrderStatusResponse order = tradeService.getBitfinexOrderStatus(id);
//		return getOrder(order);
		var op = orderPool.getById(id);
		if (op != null) {
			return op;
		}
		OrderRecord record;
		// 未約定(部分約定含む)の注文を取得
		var openOrders = getOpenOrders();
		var openOrderOption = openOrders.stream().filter(o->o.getId().equals(id)).findAny();
		if (openOrderOption.isPresent()) {
			// 取得できた場合
			record = new OrderRecord(this, openOrderOption.get());
		} else {
			BitfinexOrderStatusResponse order = tradeService.getBitfinexOrderStatus(id);
			LimitOrder limitOrder = new LimitOrder(
				order.getSide().equals("buy")?OrderType.BID:OrderType.ASK,
				order.getOriginalAmount(),
				ccyp, id, new Date(order.getTimestamp().longValue()*1000),
				order.getPrice(), order.getAvgExecutionPrice(),
				order.getExecutedAmount(),
				BigDecimal.ZERO,
				BitfinexAdapters.adaptOrderStatus(order));
			record = new OrderRecord(this, limitOrder);
		}
		var pairString = BitfinexUtils.toPairString(ccyp);
		// 取引の情報を取得
		var response = tradeService.getBitfinexOrderTradesV2(pairString, Long.parseLong(id));
		var execVol = response.stream().map(t->t.getExecAmount())
			.reduce(ZERO, BigDecimal::add).abs();
		var execVolBase = response.stream()
			.map(t->t.getExecAmount().multiply(t.getExecPrice()))
			.reduce(ZERO, BigDecimal::add).abs();
		var averagePrice = execVol.compareTo(ZERO) > 0 ?
				execVolBase.divide(execVol, scale, HALF_UP) : null;
		var origVol = record.getVolume();
		record.setExecutedVolume(execVol);
		record.setAveragePrice(averagePrice);
		return record;

	}

```
### /Users/kh/git/ccy/cryptobot/src/main/java/cryptobot/exchange/bitfinex/Bitfinex.java

```java
import static cryptobot.exchange.ExchangeService.FeeType.*;
import static java.math.BigDecimal.*;
import static java.math.RoundingMode.*;
import static java.util.Objects.*;
import static java.util.stream.Collectors.*;
import static org.knowm.xchange.dto.Order.OrderStatus.*;
import static org.knowm.xchange.dto.Order.OrderType.*;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import org.knowm.xchange.bitfinex.service.BitfinexTradeService;
import org.knowm.xchange.bitfinex.v1.BitfinexUtils;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import cryptobot.exchange.OrderbookService;

	private static final BigDecimal TRADING_CHARGE = new BigDecimal("0.00200000");
	private BitfinexTradeService tradeService;
	@Value("${bitfinex.api.key}") private String API_KEY;
	@Value("${bitfinex.api.secret}") private String SECRET;
	@Value("${bitfinex.fee.withdraw.counter}") BigDecimal COUNTER_WITHDRAW_FEE;
	@Value("${bitfinex.fee.withdraw.base}") BigDecimal BASE_WITHDRAW_FEE;
	@Value("${bitfinex.address.bch:#{null}}") String BCH_ADDRESS;
	@Value("${bitfinex.address.xlm:#{null}}") String XLM_ADDRESS;
	@Value("${bitfinex.address.xlm.memo:#{null}}") String XLM_MEMO;
	@Value("${bitfinex.rebalance.threshold.base:0}") BigDecimal rebalanceThresholdBase;
	@Value("${bitfinex.rebalance.target.base:0}") BigDecimal rebalanceTargetBase;
	@Value("${bitfinex.rebalance.threshold.counter:0}") BigDecimal rebalanceThresholdCounter;
	@Value("${bitfinex.rebalance.target.counter:0}") BigDecimal rebalanceTargetCounter;
	@Autowired BitfinexOrderbookService orderbookService;

	synchronized public List<LimitOrder> getOpenOrders() throws IOException {
//		var pairString = BitfinexUtils.toPairString(ccyp);
//		var activeOrders = tradeService.getBitfinexActiveOrdesV2(pairString);
//		var orderResult = activeOrders.stream()
//		.map(activeOrder->{
//			BigDecimal amount = activeOrder.getAmount();
//			BigDecimal orderAmount = activeOrder.getAmountOrig().abs();
//			return new LimitOrder(amount.compareTo(ZERO) < 0 ? ASK : BID,
//				orderAmount,
//				// 約定済みの数量を設定するべきだが、API使用が曖昧なため計算が正しいか検証が必要。
//				orderAmount.subtract(amount.abs()),
//				ccyp,
//				String.valueOf(activeOrder.getId()),
//				activeOrder.getTimestampCreate(),
//				requireNonNullElse(activeOrder.getPrice(), activeOrder.getPriceAvg()));
//		}).collect(toList());
//		return orderResult;
        return null;
	}

```
### /Users/kh/git/ccy/cryptobot/src/main/java/cryptobot/dto/OrderRecord.java

```java
import cryptobot.exchange.ExchangeService;
import lombok.ToString;
import org.knowm.xchange.currency.Currency;
import java.math.BigDecimal;
import java.util.*;
import static org.knowm.xchange.dto.Order.OrderStatus.*;

	private ExchangeService service;
	private HashMap<String, Object> metaInfo = new HashMap<>();
	@ToString.Exclude private Exception exception;
	private Currency feeCurrency;

	public BigDecimal getVolume() {
		return getOriginalAmount();
	}

```
### /Users/kh/git/ccy/cryptobot/src/main/java/cryptobot/dto/OrderRecord.java

```java
import cryptobot.exchange.ExchangeService;
import lombok.ToString;
import org.knowm.xchange.currency.Currency;
import java.math.BigDecimal;
import java.util.*;
import static org.knowm.xchange.dto.Order.OrderStatus.*;

	private ExchangeService service;
	private HashMap<String, Object> metaInfo = new HashMap<>();
	@ToString.Exclude private Exception exception;
	private Currency feeCurrency;

	public void setExecutedVolume(BigDecimal executedVolume) {
		setCumulativeAmount(executedVolume);
		adjustStatusByExecutionVolume();
	}

```
### /Users/kh/git/ccy/cryptobot/src/main/java/cryptobot/dto/OrderRecord.java

```java
import cryptobot.exchange.ExchangeService;
import lombok.ToString;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.dto.Order;
import java.math.BigDecimal;
import java.util.*;
import static java.math.BigDecimal.ZERO;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;
import static org.knowm.xchange.dto.Order.OrderStatus.*;

	private ExchangeService service;
	private HashMap<String, Object> metaInfo = new HashMap<>();
	@ToString.Exclude private Exception exception;
	private Currency feeCurrency;

	private void adjustStatusByExecutionVolume() {
		BigDecimal executed = defaultIfNull(getCumulativeAmount(), ZERO);
		BigDecimal total = defaultIfNull(getOriginalAmount(), ZERO);
		OrderStatus currentStatus = getStatus();

		if (currentStatus == null) return;

		if (executed.compareTo(ZERO) > 0) {
			if (total.compareTo(ZERO) > 0 && executed.compareTo(total) >= 0) {
				setOrderStatus(FILLED);
			} else if (currentStatus.equals(CANCELED)) {
				setOrderStatus(PARTIALLY_CANCELED);
			} else if (!currentStatus.isFinal()) {
				setOrderStatus(PARTIALLY_FILLED);
			}
		}
	}

```
### /Users/kh/git/ccy/cryptobot/src/main/java/cryptobot/exchange/bitflyer/Bitflyer.java

```java
import com.fasterxml.jackson.databind.ObjectMapper;
import cryptobot.dto.HealthStatus;
import cryptobot.dto.OrderRecord;
import cryptobot.exchange.OrderbookService;
import cryptobot.exchange.bitflyer.BitflyerOrder.OrderList;
import cryptobot.misc.JsonUtils;
import cryptobot.misc.MailSender;
import cryptobot.misc.Util;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order.OrderType;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

	@Value("${exchange.api.bitflyer:https://api.bitflyer.jp}")
	private String URL_BASE;
	private static final String APPLICATION_JSON = "application/json";
	private static final String ACCESS_KEY = "ACCESS-KEY";
	private static final String ACCESS_TIMESTAMP = "ACCESS-TIMESTAMP";
	private static final String ACCESS_SIGN = "ACCESS-SIGN";
	private static final String CONTENT_TYPE = "Content-Type";
	@Value("${bitflyer.api.key:#{null}}") String API_KEY;
	@Value("${bitflyer.api.secret:#{null}}") String SECRET;
	@Value("${bitflyer.fee.withdraw.counter:#{null}}") BigDecimal COUNTER_WITHDRAW_FEE;
	@Value("${bitflyer.fee.withdraw.base:#{null}}") BigDecimal BASE_WITHDRAW_FEE;
	@Value("${bitflyer.rebalance.threshold.base:0}") BigDecimal rebalanceThresholdBase;
	@Value("${bitflyer.rebalance.target.base:0}") BigDecimal rebalanceTargetBase;
	@Value("${bitflyer.rebalance.threshold.counter:0}") BigDecimal rebalanceThresholdCounter;
	@Value("${bitflyer.rebalance.target.counter:0}") BigDecimal rebalanceTargetCounter;
	@Value("${cryptobot.healthcheck}") private String enableHealthCheck;
	private HealthStatus status;
	@Autowired MailSender mail;
	@Autowired BitflyerOrderbookService orderbookService;
	@Autowired BitflyerWeb web;
	ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(
			Thread.ofVirtual().name("bf_healthcheck").factory());
	private static final ObjectMapper OBJECT_MAPPER = JsonUtils.MAPPER;
	public static enum OrderFilterBy
	public enum BitflyerOrderType
		private static final Map<String, BitflyerOrderType> fromString = new HashMap<String, BitflyerOrderType>();
	static Map<CurrencyPair, String> SUPPORTED_CCYPS = Map.of(
		CurrencyPair.BTC_JPY, "BTC_JPY",
		CurrencyPair.ETH_BTC, "ETH_BTC",
		CurrencyPair.BCH_BTC, "BCH_BTC");

	synchronized public OrderRecord getOrder(String orderKey, OrderFilterBy filterBy) throws IOException {
		String path = String.format(
			"/v1/me/getchildorders?product_code=%s&%s=%s",
			toPairString(ccyp), filterBy.toString(), orderKey);
		log.debug("Rest API Request to: {}", path);
		try {
			OrderList result = doGetWithAuth(path, OrderList.class);
			if (result.size() == 0) {
				return null;
			}
			return new OrderRecord(this, result.get(0).converLimitOrder());
		} catch (Exception e) {
			throw e;
		}
	}

```
### /Users/kh/git/ccy/cryptobot/src/main/java/cryptobot/exchange/bitflyer/Bitflyer.java

```java
import com.fasterxml.jackson.databind.ObjectMapper;
import cryptobot.dto.HealthStatus;
import cryptobot.exchange.OrderbookService;
import cryptobot.exchange.bitflyer.dto.TradeRecord.TradeList;
import cryptobot.misc.JsonUtils;
import cryptobot.misc.MailSender;
import cryptobot.misc.Util;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order.OrderType;
import org.knowm.xchange.dto.trade.UserTrade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import static java.util.stream.Collectors.toList;
import static org.knowm.xchange.dto.Order.OrderType.ASK;
import static org.knowm.xchange.dto.Order.OrderType.BID;

	@Value("${exchange.api.bitflyer:https://api.bitflyer.jp}")
	private String URL_BASE;
	private static final String APPLICATION_JSON = "application/json";
	private static final String ACCESS_KEY = "ACCESS-KEY";
	private static final String ACCESS_TIMESTAMP = "ACCESS-TIMESTAMP";
	private static final String ACCESS_SIGN = "ACCESS-SIGN";
	private static final String CONTENT_TYPE = "Content-Type";
	@Value("${bitflyer.api.key:#{null}}") String API_KEY;
	@Value("${bitflyer.api.secret:#{null}}") String SECRET;
	@Value("${bitflyer.fee.withdraw.counter:#{null}}") BigDecimal COUNTER_WITHDRAW_FEE;
	@Value("${bitflyer.fee.withdraw.base:#{null}}") BigDecimal BASE_WITHDRAW_FEE;
	@Value("${bitflyer.rebalance.threshold.base:0}") BigDecimal rebalanceThresholdBase;
	@Value("${bitflyer.rebalance.target.base:0}") BigDecimal rebalanceTargetBase;
	@Value("${bitflyer.rebalance.threshold.counter:0}") BigDecimal rebalanceThresholdCounter;
	@Value("${bitflyer.rebalance.target.counter:0}") BigDecimal rebalanceTargetCounter;
	@Value("${cryptobot.healthcheck}") private String enableHealthCheck;
	private HealthStatus status;
	@Autowired MailSender mail;
	@Autowired BitflyerOrderbookService orderbookService;
	@Autowired BitflyerWeb web;
	ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(
			Thread.ofVirtual().name("bf_healthcheck").factory());
	private static final ObjectMapper OBJECT_MAPPER = JsonUtils.MAPPER;
	public static enum OrderFilterBy
	public enum BitflyerOrderType
		private static final Map<String, BitflyerOrderType> fromString = new HashMap<String, BitflyerOrderType>();
	static Map<CurrencyPair, String> SUPPORTED_CCYPS = Map.of(
		CurrencyPair.BTC_JPY, "BTC_JPY",
		CurrencyPair.ETH_BTC, "ETH_BTC",
		CurrencyPair.BCH_BTC, "BCH_BTC");

	@Override
	public List<UserTrade> getTradesByOrderId(String childOrderId) throws IOException {
		String path = String.format(
			"/v1/me/getexecutions?child_order_id=%s&product_code=%s",
			childOrderId, toPairString(ccyp));
		log.debug("Rest API Request to: {}", path);
		try {
			TradeList result = doGetWithAuth(path, TradeList.class);
            List <UserTrade> trades = result.stream().map(bfTrade->{
				var execDate = parseDate(bfTrade.getExecDate());
				var orderType = bfTrade.getSide().equals("BUY")?OrderType.BID:OrderType.ASK;
				return UserTrade.builder()
					.type(orderType)
					.originalAmount(bfTrade.getVolume())
					.instrument(ccyp)
					.price(bfTrade.getPrice())
					.timestamp(execDate)
					.id(String.valueOf(bfTrade.getId()))
					.orderId(bfTrade.getChildOrderId())
					.feeAmount(bfTrade.getCommission())
					.feeCurrency(ccyp.getBase())
					.build();
			}).collect(toList());
			return trades;
		} catch (Exception e) {
			throw e;
		}
	}

```
### /Users/kh/git/ccy/cryptobot/src/main/java/cryptobot/exchange/bitflyer/Bitflyer.java

```java
import com.fasterxml.jackson.databind.ObjectMapper;
import cryptobot.dto.HealthStatus;
import cryptobot.exchange.OrderbookService;
import cryptobot.misc.JsonUtils;
import cryptobot.misc.MailSender;
import cryptobot.misc.Util;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order.OrderType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

	@Value("${exchange.api.bitflyer:https://api.bitflyer.jp}")
	private String URL_BASE;
	private static final String APPLICATION_JSON = "application/json";
	private static final String ACCESS_KEY = "ACCESS-KEY";
	private static final String ACCESS_TIMESTAMP = "ACCESS-TIMESTAMP";
	private static final String ACCESS_SIGN = "ACCESS-SIGN";
	private static final String CONTENT_TYPE = "Content-Type";
	@Value("${bitflyer.api.key:#{null}}") String API_KEY;
	@Value("${bitflyer.api.secret:#{null}}") String SECRET;
	@Value("${bitflyer.fee.withdraw.counter:#{null}}") BigDecimal COUNTER_WITHDRAW_FEE;
	@Value("${bitflyer.fee.withdraw.base:#{null}}") BigDecimal BASE_WITHDRAW_FEE;
	@Value("${bitflyer.rebalance.threshold.base:0}") BigDecimal rebalanceThresholdBase;
	@Value("${bitflyer.rebalance.target.base:0}") BigDecimal rebalanceTargetBase;
	@Value("${bitflyer.rebalance.threshold.counter:0}") BigDecimal rebalanceThresholdCounter;
	@Value("${bitflyer.rebalance.target.counter:0}") BigDecimal rebalanceTargetCounter;
	@Value("${cryptobot.healthcheck}") private String enableHealthCheck;
	private HealthStatus status;
	@Autowired MailSender mail;
	@Autowired BitflyerOrderbookService orderbookService;
	@Autowired BitflyerWeb web;
	ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(
			Thread.ofVirtual().name("bf_healthcheck").factory());
	private static final ObjectMapper OBJECT_MAPPER = JsonUtils.MAPPER;
	public static enum OrderFilterBy
	public enum BitflyerOrderType
		private static final Map<String, BitflyerOrderType> fromString = new HashMap<String, BitflyerOrderType>();
	static Map<CurrencyPair, String> SUPPORTED_CCYPS = Map.of(
		CurrencyPair.BTC_JPY, "BTC_JPY",
		CurrencyPair.ETH_BTC, "ETH_BTC",
		CurrencyPair.BCH_BTC, "BCH_BTC");

	public static String toPairString(CurrencyPair currencyPair) {
		return currencyPair.getBase().toString().toUpperCase() + "_" +
				currencyPair.getCounter().toString().toUpperCase();
	}

```
### /Users/kh/git/ccy/cryptobot/src/main/java/cryptobot/exchange/bitflyer/Bitflyer.java

```java
import com.fasterxml.jackson.databind.ObjectMapper;
import cryptobot.dto.HealthStatus;
import cryptobot.exchange.OrderbookService;
import cryptobot.misc.JsonUtils;
import cryptobot.misc.MailSender;
import cryptobot.misc.Util;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order.OrderType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

	@Value("${exchange.api.bitflyer:https://api.bitflyer.jp}")
	private String URL_BASE;
	private static final String APPLICATION_JSON = "application/json";
	private static final String ACCESS_KEY = "ACCESS-KEY";
	private static final String ACCESS_TIMESTAMP = "ACCESS-TIMESTAMP";
	private static final String ACCESS_SIGN = "ACCESS-SIGN";
	private static final String CONTENT_TYPE = "Content-Type";
	@Value("${bitflyer.api.key:#{null}}") String API_KEY;
	@Value("${bitflyer.api.secret:#{null}}") String SECRET;
	@Value("${bitflyer.fee.withdraw.counter:#{null}}") BigDecimal COUNTER_WITHDRAW_FEE;
	@Value("${bitflyer.fee.withdraw.base:#{null}}") BigDecimal BASE_WITHDRAW_FEE;
	@Value("${bitflyer.rebalance.threshold.base:0}") BigDecimal rebalanceThresholdBase;
	@Value("${bitflyer.rebalance.target.base:0}") BigDecimal rebalanceTargetBase;
	@Value("${bitflyer.rebalance.threshold.counter:0}") BigDecimal rebalanceThresholdCounter;
	@Value("${bitflyer.rebalance.target.counter:0}") BigDecimal rebalanceTargetCounter;
	@Value("${cryptobot.healthcheck}") private String enableHealthCheck;
	private HealthStatus status;
	@Autowired MailSender mail;
	@Autowired BitflyerOrderbookService orderbookService;
	@Autowired BitflyerWeb web;
	ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(
			Thread.ofVirtual().name("bf_healthcheck").factory());
	private static final ObjectMapper OBJECT_MAPPER = JsonUtils.MAPPER;
	public static enum OrderFilterBy
	public enum BitflyerOrderType
		private static final Map<String, BitflyerOrderType> fromString = new HashMap<String, BitflyerOrderType>();
	static Map<CurrencyPair, String> SUPPORTED_CCYPS = Map.of(
		CurrencyPair.BTC_JPY, "BTC_JPY",
		CurrencyPair.ETH_BTC, "ETH_BTC",
		CurrencyPair.BCH_BTC, "BCH_BTC");

	private <T> T doGetWithAuth(String path, ParameterizedTypeReference<T> typeReference) {
		String timestamp = String.valueOf(new Date().getTime());
		String data = timestamp + "GET" + path;
		String hash = createHMAC(SECRET, data);
		return restClient.get()
				.uri(URL_BASE + path)
				.header(ACCESS_KEY, API_KEY)
				.header(ACCESS_TIMESTAMP, timestamp)
				.header(ACCESS_SIGN, hash)
				.retrieve()
				.body(typeReference);
	}

```
### /Users/kh/git/ccy/cryptobot/src/main/java/cryptobot/exchange/bitflyer/Bitflyer.java

```java
import com.fasterxml.jackson.databind.ObjectMapper;
import cryptobot.dto.HealthStatus;
import cryptobot.exchange.OrderbookService;
import cryptobot.misc.JsonUtils;
import cryptobot.misc.MailSender;
import cryptobot.misc.Util;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order.OrderType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

	@Value("${exchange.api.bitflyer:https://api.bitflyer.jp}")
	private String URL_BASE;
	private static final String APPLICATION_JSON = "application/json";
	private static final String ACCESS_KEY = "ACCESS-KEY";
	private static final String ACCESS_TIMESTAMP = "ACCESS-TIMESTAMP";
	private static final String ACCESS_SIGN = "ACCESS-SIGN";
	private static final String CONTENT_TYPE = "Content-Type";
	@Value("${bitflyer.api.key:#{null}}") String API_KEY;
	@Value("${bitflyer.api.secret:#{null}}") String SECRET;
	@Value("${bitflyer.fee.withdraw.counter:#{null}}") BigDecimal COUNTER_WITHDRAW_FEE;
	@Value("${bitflyer.fee.withdraw.base:#{null}}") BigDecimal BASE_WITHDRAW_FEE;
	@Value("${bitflyer.rebalance.threshold.base:0}") BigDecimal rebalanceThresholdBase;
	@Value("${bitflyer.rebalance.target.base:0}") BigDecimal rebalanceTargetBase;
	@Value("${bitflyer.rebalance.threshold.counter:0}") BigDecimal rebalanceThresholdCounter;
	@Value("${bitflyer.rebalance.target.counter:0}") BigDecimal rebalanceTargetCounter;
	@Value("${cryptobot.healthcheck}") private String enableHealthCheck;
	private HealthStatus status;
	@Autowired MailSender mail;
	@Autowired BitflyerOrderbookService orderbookService;
	@Autowired BitflyerWeb web;
	ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(
			Thread.ofVirtual().name("bf_healthcheck").factory());
	private static final ObjectMapper OBJECT_MAPPER = JsonUtils.MAPPER;
	public static enum OrderFilterBy
	public enum BitflyerOrderType
		private static final Map<String, BitflyerOrderType> fromString = new HashMap<String, BitflyerOrderType>();
	static Map<CurrencyPair, String> SUPPORTED_CCYPS = Map.of(
		CurrencyPair.BTC_JPY, "BTC_JPY",
		CurrencyPair.ETH_BTC, "ETH_BTC",
		CurrencyPair.BCH_BTC, "BCH_BTC");

	public static Date parseDate(String datetime) {
		var df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
		try {
			return df.parse(datetime+"+0000");
		} catch (ParseException e) {
			return null;
		}
	}

```
### /Users/kh/git/ccy/cryptobot/src/main/java/cryptobot/misc/Util.java

```java
import static java.math.BigDecimal.*;
import static java.math.RoundingMode.*;
import static org.knowm.xchange.dto.Order.OrderType.*;

	public static void sleep(long milli) {
		try {
			Thread.sleep(milli);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

```
### /Users/kh/git/ccy/cryptobot/src/main/java/cryptobot/exchange/bitflyer/BitflyerFX.java

```java
import static java.math.BigDecimal.*;
import static java.math.RoundingMode.*;
import static org.knowm.xchange.currency.Currency.*;
import static org.knowm.xchange.dto.Order.OrderType.*;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import static java.util.stream.Collectors.joining;
import cryptobot.exchange.bitflyer.BitflyerOrder.OrderList;
import org.knowm.xchange.dto.Order.OrderType;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import cryptobot.dto.OrderRecord;
import cryptobot.exchange.OrderbookService;

	private static final String HMAC_SHA256 = "HmacSHA256";
	private static final String URL_BASE = "https://api.bitflyer.jp";
	private static final String APPLICATION_JSON = "application/json";
	private static final String ACCESS_KEY = "ACCESS-KEY";
	private static final String ACCESS_TIMESTAMP = "ACCESS-TIMESTAMP";
	private static final String ACCESS_SIGN = "ACCESS-SIGN";
	private static final String CONTENT_TYPE = "Content-Type";
	private static final String ccyps = "FX_BTC_JPY";
	@Value("${bitflyer.api.key:#{null}}") String API_KEY;
	@Value("${bitflyer.api.secret:#{null}}") String SECRET;
	BigDecimal TRADING_CHARGE = ZERO;
	@Autowired BitflyerFxOrderbookService orderbookService;
	@Autowired Bitflyer bitflyer;
		public OrderType type;
		public BigDecimal price;
		public BigDecimal volume;
		public BigDecimal collateral;
		public Date openDate;
		public Integer leverage;
		public BigDecimal pnl;
		public BigDecimal sfd;

	public OrderRecord getOrder(String childOrderAcceptanceId) throws IOException {
		var params = Map.of("product_code", ccyps, "count", "100");
		var path = "/v1/me/getchildorders?" + params.entrySet().stream()
				.map(e->e.getKey()+"="+e.getValue()).collect(joining("&"));
		try {
			OrderList result = doGetWithAuth(path, OrderList.class);
			if (childOrderAcceptanceId == null) return new OrderRecord(this, result.get(0).converLimitOrder());
			OrderList edittedResult = new OrderList();
			result.stream()
				.filter(n->n.getChildOrderAcceptanceId().equals(childOrderAcceptanceId))
				.forEach(n->{edittedResult.add(n);});
			Optional<OrderRecord> order = edittedResult.stream()
				.map(bo->new OrderRecord(this, bo.converLimitOrder()))
				.findAny();
			if (order.isEmpty()) return null;
			OrderRecord orderRecord = order.get();
			return orderRecord;
		} catch (Exception e) {
			throw new IOException(e);
		}
	}

```
### /Users/kh/git/ccy/cryptobot/src/main/java/cryptobot/exchange/bitflyer/BitflyerFX.java

```java
import static java.math.BigDecimal.*;
import static java.math.RoundingMode.*;
import static org.knowm.xchange.currency.Currency.*;
import static org.knowm.xchange.dto.Order.OrderType.*;
import java.math.BigDecimal;
import java.util.*;
import org.knowm.xchange.dto.Order.OrderType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.core.ParameterizedTypeReference;
import cryptobot.exchange.OrderbookService;

	private static final String HMAC_SHA256 = "HmacSHA256";
	private static final String URL_BASE = "https://api.bitflyer.jp";
	private static final String APPLICATION_JSON = "application/json";
	private static final String ACCESS_KEY = "ACCESS-KEY";
	private static final String ACCESS_TIMESTAMP = "ACCESS-TIMESTAMP";
	private static final String ACCESS_SIGN = "ACCESS-SIGN";
	private static final String CONTENT_TYPE = "Content-Type";
	private static final String ccyps = "FX_BTC_JPY";
	@Value("${bitflyer.api.key:#{null}}") String API_KEY;
	@Value("${bitflyer.api.secret:#{null}}") String SECRET;
	BigDecimal TRADING_CHARGE = ZERO;
	@Autowired BitflyerFxOrderbookService orderbookService;
	@Autowired Bitflyer bitflyer;
		public OrderType type;
		public BigDecimal price;
		public BigDecimal volume;
		public BigDecimal collateral;
		public Date openDate;
		public Integer leverage;
		public BigDecimal pnl;
		public BigDecimal sfd;

	private <T> T doGetWithAuth(String path, ParameterizedTypeReference<T> typeReference) throws Exception {
		String timestamp = String.valueOf(new Date().getTime());
		char[] hash = createHMAC("GET", path, timestamp, null);
		return restClient.get()
				.uri(URL_BASE + path)
				.header(ACCESS_KEY, API_KEY)
				.header(ACCESS_TIMESTAMP, timestamp)
				.header(ACCESS_SIGN, new String(hash))
				.retrieve()
				.body(typeReference);
	}

```
### /Users/kh/git/ccy/cryptobot/src/main/java/cryptobot/exchange/bitflyer/BitflyerFX.java

```java
import static java.math.BigDecimal.*;
import static java.math.RoundingMode.*;
import static org.knowm.xchange.currency.Currency.*;
import static org.knowm.xchange.dto.Order.OrderType.*;
import java.math.BigDecimal;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.binary.Hex;
import org.knowm.xchange.dto.Order.OrderType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import cryptobot.exchange.OrderbookService;

	private static final String HMAC_SHA256 = "HmacSHA256";
	private static final String URL_BASE = "https://api.bitflyer.jp";
	private static final String APPLICATION_JSON = "application/json";
	private static final String ACCESS_KEY = "ACCESS-KEY";
	private static final String ACCESS_TIMESTAMP = "ACCESS-TIMESTAMP";
	private static final String ACCESS_SIGN = "ACCESS-SIGN";
	private static final String CONTENT_TYPE = "Content-Type";
	private static final String ccyps = "FX_BTC_JPY";
	@Value("${bitflyer.api.key:#{null}}") String API_KEY;
	@Value("${bitflyer.api.secret:#{null}}") String SECRET;
	BigDecimal TRADING_CHARGE = ZERO;
	@Autowired BitflyerFxOrderbookService orderbookService;
	@Autowired Bitflyer bitflyer;
		public OrderType type;
		public BigDecimal price;
		public BigDecimal volume;
		public BigDecimal collateral;
		public Date openDate;
		public Integer leverage;
		public BigDecimal pnl;
		public BigDecimal sfd;

	private char[] createHMAC(String method, String path, String timestamp, String body) throws NoSuchAlgorithmException, InvalidKeyException {
		Mac sha256_HMAC = Mac.getInstance(HMAC_SHA256);
		SecretKeySpec secret_key = new SecretKeySpec(SECRET.getBytes() , HMAC_SHA256);
		sha256_HMAC.init(secret_key);
		String text = timestamp + method + path + (body==null?"":body);
		sha256_HMAC.update(text.getBytes());
		char[] hash = Hex.encodeHex(sha256_HMAC.doFinal());
		return hash;
	}

```
### /Users/kh/git/ccy/cryptobot/src/main/java/cryptobot/exchange/bitpoint/Bitpoint.java

```java
import static java.math.BigDecimal.*;
import static java.util.stream.Collectors.*;
import static org.apache.commons.lang3.StringUtils.*;
import static org.knowm.xchange.currency.CurrencyPair.*;
import static org.knowm.xchange.dto.Order.OrderType.*;
import java.math.BigDecimal;
import java.util.Map;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.JsonNode;
import cryptobot.dto.OrderRecord;
import cryptobot.exchange.OrderbookService;
import cryptobot.misc.MailSender;

	private static final String URL_BASE = "https://smartapi.bitpoint.co.jp/bpj-smart-api";
	private static final String APPLICATION_JSON = "application/json";
	private static final String ACCESS_KEY = "access-key";
	private static final String ACCESS_NONCE = "access-nonce";
	private static final String ACCESS_SIGN = "access-signature";
	private static final String CONTENT_TYPE = "Content-Type";
	@Value("${bitpoint.api.key:#{null}}") String API_KEY;
	@Value("${bitpoint.api.secret:#{null}}") String SECRET;
	@Value("${bitpoint.pincode:#{null}}") String PIN_CODE;
	@Value("${bitpoint.rebalance.threshold.base:0}") BigDecimal rebalanceThresholdBase;
	@Value("${bitpoint.rebalance.target.base:0}") BigDecimal rebalanceTargetBase;
	@Value("${bitpoint.rebalance.threshold.counter:0}") BigDecimal rebalanceThresholdCounter;
	@Value("${bitpoint.rebalance.target.counter:0}") BigDecimal rebalanceTargetCounter;
	@Autowired MailSender mail;
	@Autowired BitpointOrderbookService orderbookService;
	static Map<CurrencyPair, String> SUPPORTED_CCYP = Map.of(
			CurrencyPair.BTC_JPY, "BTCJPY",
			CurrencyPair.ETH_JPY, "ETHJPY");
	@Autowired BitpointWeb web;

	@Override
	public OrderRecord getOrder(String id) throws Exception {
		var path = "/api/singleOrder";
		var params = Map.of(
				"symbol", SUPPORTED_CCYP.get(BTC_JPY),
				"orderId", id,
				"tradeType", "SPOT");
		JsonNode json = doHttpGet(path, params);
		log.info("order info : {}", json);
		var order = createLimitOrder(json);
		return new OrderRecord(this, order);
	}

```
### /Users/kh/git/ccy/cryptobot/src/main/java/cryptobot/exchange/bitpoint/Bitpoint.java

```java
import static java.math.BigDecimal.*;
import static java.util.stream.Collectors.*;
import static org.apache.commons.lang3.StringUtils.*;
import static org.knowm.xchange.currency.CurrencyPair.*;
import static org.knowm.xchange.dto.Order.OrderType.*;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.JsonNode;
import cryptobot.exchange.OrderbookService;
import cryptobot.misc.MailSender;
import cryptobot.misc.JsonUtils;

	private static final String URL_BASE = "https://smartapi.bitpoint.co.jp/bpj-smart-api";
	private static final String APPLICATION_JSON = "application/json";
	private static final String ACCESS_KEY = "access-key";
	private static final String ACCESS_NONCE = "access-nonce";
	private static final String ACCESS_SIGN = "access-signature";
	private static final String CONTENT_TYPE = "Content-Type";
	@Value("${bitpoint.api.key:#{null}}") String API_KEY;
	@Value("${bitpoint.api.secret:#{null}}") String SECRET;
	@Value("${bitpoint.pincode:#{null}}") String PIN_CODE;
	@Value("${bitpoint.rebalance.threshold.base:0}") BigDecimal rebalanceThresholdBase;
	@Value("${bitpoint.rebalance.target.base:0}") BigDecimal rebalanceTargetBase;
	@Value("${bitpoint.rebalance.threshold.counter:0}") BigDecimal rebalanceThresholdCounter;
	@Value("${bitpoint.rebalance.target.counter:0}") BigDecimal rebalanceTargetCounter;
	@Autowired MailSender mail;
	@Autowired BitpointOrderbookService orderbookService;
	static Map<CurrencyPair, String> SUPPORTED_CCYP = Map.of(
			CurrencyPair.BTC_JPY, "BTCJPY",
			CurrencyPair.ETH_JPY, "ETHJPY");
	@Autowired BitpointWeb web;

	private JsonNode doHttpGet(String path, Map<String, String> params) throws Exception {
		long nonce = System.currentTimeMillis();
		var requestParam = new HashMap<>(params);
		requestParam.put("timestamp", String.valueOf(nonce));
		requestParam.put("recvWindow", "10000");
		
		var paramString = requestParam.entrySet().stream()
				.map(e -> e.getKey() + "=" + e.getValue())
				.collect(joining("&"));
		
		var url = URL_BASE + path + (isEmpty(paramString) ? "" : "?" + paramString);
		var message = API_KEY + "\n" + nonce + "\n" + (paramString == null ? "" : paramString);
		var signature = createHMAC(SECRET, message);

		String body = restClient.get()
				.uri(url)
				.header(ACCESS_KEY, API_KEY)
				.header(ACCESS_NONCE, String.valueOf(nonce))
				.header(ACCESS_SIGN, signature)
				.header(CONTENT_TYPE, APPLICATION_JSON)
				.retrieve()
				.body(String.class);
		return JsonUtils.MAPPER.readTree(body);
	}

```
### /Users/kh/git/ccy/cryptobot/src/main/java/cryptobot/exchange/bitpoint/Bitpoint.java

```java
import static java.math.BigDecimal.*;
import static java.util.stream.Collectors.*;
import static org.apache.commons.lang3.StringUtils.*;
import static org.knowm.xchange.currency.CurrencyPair.*;
import static org.knowm.xchange.dto.Order.OrderType.*;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.JsonNode;
import cryptobot.exchange.OrderbookService;
import cryptobot.misc.MailSender;

	private static final String URL_BASE = "https://smartapi.bitpoint.co.jp/bpj-smart-api";
	private static final String APPLICATION_JSON = "application/json";
	private static final String ACCESS_KEY = "access-key";
	private static final String ACCESS_NONCE = "access-nonce";
	private static final String ACCESS_SIGN = "access-signature";
	private static final String CONTENT_TYPE = "Content-Type";
	@Value("${bitpoint.api.key:#{null}}") String API_KEY;
	@Value("${bitpoint.api.secret:#{null}}") String SECRET;
	@Value("${bitpoint.pincode:#{null}}") String PIN_CODE;
	@Value("${bitpoint.rebalance.threshold.base:0}") BigDecimal rebalanceThresholdBase;
	@Value("${bitpoint.rebalance.target.base:0}") BigDecimal rebalanceTargetBase;
	@Value("${bitpoint.rebalance.threshold.counter:0}") BigDecimal rebalanceThresholdCounter;
	@Value("${bitpoint.rebalance.target.counter:0}") BigDecimal rebalanceTargetCounter;
	@Autowired MailSender mail;
	@Autowired BitpointOrderbookService orderbookService;
	static Map<CurrencyPair, String> SUPPORTED_CCYP = Map.of(
			CurrencyPair.BTC_JPY, "BTCJPY",
			CurrencyPair.ETH_JPY, "ETHJPY");
	@Autowired BitpointWeb web;

	private LimitOrder createLimitOrder(JsonNode json) {
		var orderId = json.path("orderId").asText();
		var type = json.path("side").asText().equals("SELL") ? ASK : BID;
		var executedVol = new BigDecimal(json.path("executedQty").asText("0"));
		var totalVolume = new BigDecimal(json.path("orderQty").asText("0"));
		var price = new BigDecimal(json.path("price").asText("0"));
		var timestamp = new Date(json.path("time").asLong());
		var status = adaptStatus(json.path("status").asText());
		var symbol = json.path("symbol").asText("");
		var ccyp = new CurrencyPair(symbol.substring(0, 3), symbol.substring(3, 6));
		return new LimitOrder(type, totalVolume, ccyp, orderId,
				timestamp, price, null, executedVol, ZERO, status);
	}

```
### /Users/kh/git/ccy/cryptobot/src/main/java/cryptobot/exchange/bitpoint/Bitpoint.java

```java
import static java.math.BigDecimal.*;
import static java.util.stream.Collectors.*;
import static org.apache.commons.lang3.StringUtils.*;
import static org.knowm.xchange.currency.CurrencyPair.*;
import static org.knowm.xchange.dto.Order.OrderType.*;
import java.math.BigDecimal;
import java.util.Map;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.Order.OrderStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import cryptobot.exchange.OrderbookService;
import cryptobot.misc.MailSender;

	private static final String URL_BASE = "https://smartapi.bitpoint.co.jp/bpj-smart-api";
	private static final String APPLICATION_JSON = "application/json";
	private static final String ACCESS_KEY = "access-key";
	private static final String ACCESS_NONCE = "access-nonce";
	private static final String ACCESS_SIGN = "access-signature";
	private static final String CONTENT_TYPE = "Content-Type";
	@Value("${bitpoint.api.key:#{null}}") String API_KEY;
	@Value("${bitpoint.api.secret:#{null}}") String SECRET;
	@Value("${bitpoint.pincode:#{null}}") String PIN_CODE;
	@Value("${bitpoint.rebalance.threshold.base:0}") BigDecimal rebalanceThresholdBase;
	@Value("${bitpoint.rebalance.target.base:0}") BigDecimal rebalanceTargetBase;
	@Value("${bitpoint.rebalance.threshold.counter:0}") BigDecimal rebalanceThresholdCounter;
	@Value("${bitpoint.rebalance.target.counter:0}") BigDecimal rebalanceTargetCounter;
	@Autowired MailSender mail;
	@Autowired BitpointOrderbookService orderbookService;
	static Map<CurrencyPair, String> SUPPORTED_CCYP = Map.of(
			CurrencyPair.BTC_JPY, "BTCJPY",
			CurrencyPair.ETH_JPY, "ETHJPY");
	@Autowired BitpointWeb web;

	private static OrderStatus adaptStatus(String status) {
		// ステータス(1：執行中、2：執行済、3：約定、5：訂正中、6：訂正済、7：取消中、8：取消済、9：出来ず、10：逆指値)
		if (status.equals("1") || status.equals("2") ||
				status.equals("5") || status.equals("6") || status.equals("ACCEPTED")) {
			return OrderStatus.NEW;
		} else if (status.equals("3")) {
			return OrderStatus.FILLED;
		} else if (status.equals("7") || status.equals("8") || status.equals("9") || status.equals("CANCEL")) {
			return OrderStatus.CANCELED;
		} else {
			return OrderStatus.FILLED;
		}
	}

```
### /Users/kh/git/ccy/cryptobot/src/main/java/cryptobot/exchange/binance/Binance.java

```java
import cryptobot.dto.OrderRecord;
import cryptobot.exchange.OrderbookService;
import org.knowm.xchange.binance.service.BinanceAccountService;
import org.knowm.xchange.binance.service.BinanceTradeService;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.Order.OrderType;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.knowm.xchange.dto.trade.LimitOrder.Builder;
import org.knowm.xchange.service.trade.TradeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.*;
import static cryptobot.exchange.ExchangeService.FeeType.MAKER;
import static java.math.BigDecimal.ONE;
import static java.math.RoundingMode.HALF_UP;
import static org.knowm.xchange.dto.Order.OrderType.BID;

	@Autowired BinanceOrderbookService orderbookService;
	@Value("${binance.api.key}") private String API_KEY;
	@Value("${binance.api.secret}") private String SECRET;
	@Value("${binance.fee.withdraw.counter}") BigDecimal COUNTER_WITHDRAW_FEE;
	@Value("${binance.fee.withdraw.base}") BigDecimal BASE_WITHDRAW_FEE;
	@Value("${binance.address.eth:#{null}}") String ETH_ADDRESS;
	@Value("${binance.address.btc:#{null}}") String BTC_ADDRESS;
	@Value("${binance.address.bch:#{null}}") String BCH_ADDRESS;
	@Value("${binance.address.xlm:#{null}}") String XLM_ADDRESS;
	@Value("${binance.address.xlm.memo:#{null}}") String XLM_MEMO;
	@Value("${binance.rebalance.threshold.base:0}") BigDecimal rebalanceThresholdBase;
	@Value("${binance.rebalance.target.base:0}") BigDecimal rebalanceTargetBase;
	@Value("${binance.rebalance.threshold.counter:0}") BigDecimal rebalanceThresholdCounter;
	@Value("${binance.rebalance.target.counter:0}") BigDecimal rebalanceTargetCounter;
	BinanceTradeService tradeService;
	BinanceAccountService accountService;
	private final int stepSize = 4;
	@Value("${exchange.api.binance:#{null}}") private String API_ENDPOINT;

	@Override
	synchronized public OrderRecord buySpot(BigDecimal volume, BigDecimal limitPrice) throws Exception {
		// 取得通貨で手数料を徴収されるため、取引金額(volume)に手数分を上乗せて受け渡す。
		volume = volume.divide(ONE.subtract(getFeeRate(MAKER)), stepSize, HALF_UP);
		LimitOrder limitOrder = new Builder(OrderType.BID, ccyp)
				.limitPrice(limitPrice).originalAmount(volume.setScale(stepSize, HALF_UP)).build();
		String orderId = tradeService.placeLimitOrder(limitOrder);
		log.info("order sent. id: {}", orderId);
		return waitOrderCreate(orderId);
	}

```
### /Users/kh/git/ccy/cryptobot/src/main/java/cryptobot/exchange/ExchangeService.java

```java
import static java.math.BigDecimal.*;
import static java.math.RoundingMode.*;
import static java.util.Optional.*;
import static java.util.stream.Collectors.*;
import static org.knowm.xchange.dto.Order.OrderStatus.*;
import static org.knowm.xchange.dto.Order.OrderType.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import cryptobot.dto.*;
import org.springframework.web.client.RestClient;
import org.knowm.xchange.BaseExchange;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.account.Balance;
import org.knowm.xchange.dto.account.Fee;
import org.knowm.xchange.dto.marketdata.Ticker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import cryptobot.AppConfig;
import cryptobot.misc.MailSender;

	@Value("${cryptobot.currencyPair}") protected CurrencyPair ccyp;
	@Value("${cryptobot.price.precision}") protected int scale;
	@Value("${cryptobot.volume.precision}") protected int volScale;
	protected BaseExchange xchange;
	protected final ConcurrentHashMap<Currency, BalanceInfo> balanceInfoMap = new ConcurrentHashMap<>();
	protected final OrderPool orderPool = new OrderPool();
	protected volatile Ticker lastTick;
	protected final Logger log = LoggerFactory.getLogger(this.getClass());
	private boolean enable = true;
	private boolean allowBid = true;
	private boolean allowAsk = true;
	@Autowired ApplicationEventPublisher publisher;
	@Autowired protected AppConfig config;
	@Autowired protected RestClient restClient;
	@Autowired MailSender mailer;
	protected Fee fee = new Fee(ZERO, ZERO);
	public static enum FeeType
	@Autowired TickerLogger tickerLogger;
		final Set<String> expectedOrderId = new HashSet<>();

	public BigDecimal getFeeRate(FeeType feeType) {
		if (feeType.equals(FeeType.MAKER)) {
			return fee.getMakerFee();
		} else {
			return fee.getTakerFee();
		}
	}

```
### /Users/kh/git/ccy/cryptobot/src/main/java/cryptobot/exchange/ExchangeService.java

```java
import static java.math.BigDecimal.*;
import static java.math.RoundingMode.*;
import static java.util.Optional.*;
import static java.util.stream.Collectors.*;
import static org.knowm.xchange.dto.Order.OrderStatus.*;
import static org.knowm.xchange.dto.Order.OrderType.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import cryptobot.dto.*;
import org.springframework.web.client.RestClient;
import org.knowm.xchange.BaseExchange;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.account.Balance;
import org.knowm.xchange.dto.account.Fee;
import org.knowm.xchange.dto.marketdata.Ticker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import cryptobot.AppConfig;
import cryptobot.misc.MailSender;

	@Value("${cryptobot.currencyPair}") protected CurrencyPair ccyp;
	@Value("${cryptobot.price.precision}") protected int scale;
	@Value("${cryptobot.volume.precision}") protected int volScale;
	protected BaseExchange xchange;
	protected final ConcurrentHashMap<Currency, BalanceInfo> balanceInfoMap = new ConcurrentHashMap<>();
	protected final OrderPool orderPool = new OrderPool();
	protected volatile Ticker lastTick;
	protected final Logger log = LoggerFactory.getLogger(this.getClass());
	private boolean enable = true;
	private boolean allowBid = true;
	private boolean allowAsk = true;
	@Autowired ApplicationEventPublisher publisher;
	@Autowired protected AppConfig config;
	@Autowired protected RestClient restClient;
	@Autowired MailSender mailer;
	protected Fee fee = new Fee(ZERO, ZERO);
	public static enum FeeType
	@Autowired TickerLogger tickerLogger;
		final Set<String> expectedOrderId = new HashSet<>();

		synchronized public OrderRecord waitOrderClosed(String orderId) throws InterruptedException {
			var startTime = System.currentTimeMillis();
			while (System.currentTimeMillis() - startTime < 10*1000L) {
				var order = getById(orderId);
				if (order != null && order.getStatus() != null) {
					if (!order.isActive()) {
						return order;
					}
				}
				expectedOrderId.add(orderId);
				this.wait(3000L);
				expectedOrderId.remove(orderId);
			}
			return null;
		}

```
### /Users/kh/git/ccy/cryptobot/src/main/java/cryptobot/exchange/ExchangeService.java

```java
import static java.math.BigDecimal.*;
import static java.math.RoundingMode.*;
import static java.util.Optional.*;
import static java.util.stream.Collectors.*;
import static org.knowm.xchange.dto.Order.OrderStatus.*;
import static org.knowm.xchange.dto.Order.OrderType.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import cryptobot.dto.*;
import org.springframework.web.client.RestClient;
import org.knowm.xchange.BaseExchange;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.account.Balance;
import org.knowm.xchange.dto.account.Fee;
import org.knowm.xchange.dto.marketdata.Ticker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import cryptobot.AppConfig;
import cryptobot.misc.MailSender;

	@Value("${cryptobot.currencyPair}") protected CurrencyPair ccyp;
	@Value("${cryptobot.price.precision}") protected int scale;
	@Value("${cryptobot.volume.precision}") protected int volScale;
	protected BaseExchange xchange;
	protected final ConcurrentHashMap<Currency, BalanceInfo> balanceInfoMap = new ConcurrentHashMap<>();
	protected final OrderPool orderPool = new OrderPool();
	protected volatile Ticker lastTick;
	protected final Logger log = LoggerFactory.getLogger(this.getClass());
	private boolean enable = true;
	private boolean allowBid = true;
	private boolean allowAsk = true;
	@Autowired ApplicationEventPublisher publisher;
	@Autowired protected AppConfig config;
	@Autowired protected RestClient restClient;
	@Autowired MailSender mailer;
	protected Fee fee = new Fee(ZERO, ZERO);
	public static enum FeeType
	@Autowired TickerLogger tickerLogger;
		final Set<String> expectedOrderId = new HashSet<>();

		synchronized public OrderRecord waitOrderClosed(String orderId) throws InterruptedException {
			var startTime = System.currentTimeMillis();
			while (System.currentTimeMillis() - startTime < 10*1000L) {
				var order = getById(orderId);
				if (order != null && order.getStatus() != null) {
					if (!order.isActive()) {
						return order;
					}
				}
				expectedOrderId.add(orderId);
				this.wait(3000L);
				expectedOrderId.remove(orderId);
			}
			return null;
		}

```
### /Users/kh/git/ccy/cryptobot/src/main/java/cryptobot/exchange/ExchangeService.java

```java
import static java.math.BigDecimal.*;
import static java.math.RoundingMode.*;
import static java.util.Optional.*;
import static java.util.stream.Collectors.*;
import static org.knowm.xchange.dto.Order.OrderStatus.*;
import static org.knowm.xchange.dto.Order.OrderType.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import cryptobot.dto.*;
import org.springframework.web.client.RestClient;
import org.knowm.xchange.BaseExchange;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.account.Balance;
import org.knowm.xchange.dto.account.Fee;
import org.knowm.xchange.dto.marketdata.Ticker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import cryptobot.AppConfig;
import cryptobot.misc.MailSender;

	@Value("${cryptobot.currencyPair}") protected CurrencyPair ccyp;
	@Value("${cryptobot.price.precision}") protected int scale;
	@Value("${cryptobot.volume.precision}") protected int volScale;
	protected BaseExchange xchange;
	protected final ConcurrentHashMap<Currency, BalanceInfo> balanceInfoMap = new ConcurrentHashMap<>();
	protected final OrderPool orderPool = new OrderPool();
	protected volatile Ticker lastTick;
	protected final Logger log = LoggerFactory.getLogger(this.getClass());
	private boolean enable = true;
	private boolean allowBid = true;
	private boolean allowAsk = true;
	@Autowired ApplicationEventPublisher publisher;
	@Autowired protected AppConfig config;
	@Autowired protected RestClient restClient;
	@Autowired MailSender mailer;
	protected Fee fee = new Fee(ZERO, ZERO);
	public static enum FeeType
	@Autowired TickerLogger tickerLogger;
		final Set<String> expectedOrderId = new HashSet<>();

		synchronized public OrderRecord getById(String id) {
			return get(id);
		}

```
### /Users/kh/git/ccy/cryptobot/src/main/java/cryptobot/exchange/ExchangeService.java

```java
import static java.math.BigDecimal.*;
import static java.math.RoundingMode.*;
import static java.util.Optional.*;
import static java.util.stream.Collectors.*;
import static org.knowm.xchange.dto.Order.OrderStatus.*;
import static org.knowm.xchange.dto.Order.OrderType.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import cryptobot.dto.*;
import org.springframework.web.client.RestClient;
import org.knowm.xchange.BaseExchange;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.account.Balance;
import org.knowm.xchange.dto.account.Fee;
import org.knowm.xchange.dto.marketdata.Ticker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import cryptobot.AppConfig;
import cryptobot.misc.MailSender;

	@Value("${cryptobot.currencyPair}") protected CurrencyPair ccyp;
	@Value("${cryptobot.price.precision}") protected int scale;
	@Value("${cryptobot.volume.precision}") protected int volScale;
	protected BaseExchange xchange;
	protected final ConcurrentHashMap<Currency, BalanceInfo> balanceInfoMap = new ConcurrentHashMap<>();
	protected final OrderPool orderPool = new OrderPool();
	protected volatile Ticker lastTick;
	protected final Logger log = LoggerFactory.getLogger(this.getClass());
	private boolean enable = true;
	private boolean allowBid = true;
	private boolean allowAsk = true;
	@Autowired ApplicationEventPublisher publisher;
	@Autowired protected AppConfig config;
	@Autowired protected RestClient restClient;
	@Autowired MailSender mailer;
	protected Fee fee = new Fee(ZERO, ZERO);
	public static enum FeeType
	@Autowired TickerLogger tickerLogger;
		final Set<String> expectedOrderId = new HashSet<>();

		synchronized public OrderRecord getById(String id) {
			return get(id);
		}

```
### /Users/kh/git/ccy/cryptobot/src/main/java/cryptobot/exchange/ExchangeService.java

```java
import static java.math.BigDecimal.*;
import static java.math.RoundingMode.*;
import static java.util.Optional.*;
import static java.util.stream.Collectors.*;
import static org.knowm.xchange.dto.Order.OrderStatus.*;
import static org.knowm.xchange.dto.Order.OrderType.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import cryptobot.dto.*;
import org.springframework.web.client.RestClient;
import org.knowm.xchange.BaseExchange;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.account.Balance;
import org.knowm.xchange.dto.account.Fee;
import org.knowm.xchange.dto.marketdata.Ticker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import cryptobot.AppConfig;
import cryptobot.misc.MailSender;

	@Value("${cryptobot.currencyPair}") protected CurrencyPair ccyp;
	@Value("${cryptobot.price.precision}") protected int scale;
	@Value("${cryptobot.volume.precision}") protected int volScale;
	protected BaseExchange xchange;
	protected final ConcurrentHashMap<Currency, BalanceInfo> balanceInfoMap = new ConcurrentHashMap<>();
	protected final OrderPool orderPool = new OrderPool();
	protected volatile Ticker lastTick;
	protected final Logger log = LoggerFactory.getLogger(this.getClass());
	private boolean enable = true;
	private boolean allowBid = true;
	private boolean allowAsk = true;
	@Autowired ApplicationEventPublisher publisher;
	@Autowired protected AppConfig config;
	@Autowired protected RestClient restClient;
	@Autowired MailSender mailer;
	protected Fee fee = new Fee(ZERO, ZERO);
	public static enum FeeType
	@Autowired TickerLogger tickerLogger;
		final Set<String> expectedOrderId = new HashSet<>();

	/**
	 * 指値注文発行後の取得待ち(注文発行後は即時に取得できない)
	 * @param orderId
	 * @return
	 * @throws Exception
	 */
	protected OrderRecord waitOrderCreate(String orderId) throws Exception {
		var order = orderPool.waitOrder(orderId, null);
		if (order != null) {
			return new OrderRecord(this, order);
		} else {
			// 取引所過負荷により通知が大幅に遅延する場合があるためRESTで取得した注文情報をorderPoolに格納
			order = getOrder(orderId);
			orderPool.update(order);
			return order;
		}
	}

```
### /Users/kh/git/ccy/cryptobot/src/main/java/cryptobot/exchange/ExchangeService.java

```java
import static java.math.BigDecimal.*;
import static java.math.RoundingMode.*;
import static java.util.Optional.*;
import static java.util.stream.Collectors.*;
import static org.knowm.xchange.dto.Order.OrderStatus.*;
import static org.knowm.xchange.dto.Order.OrderType.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import cryptobot.dto.*;
import org.springframework.web.client.RestClient;
import org.knowm.xchange.BaseExchange;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.Order.OrderStatus;
import org.knowm.xchange.dto.account.Balance;
import org.knowm.xchange.dto.account.Fee;
import org.knowm.xchange.dto.marketdata.Ticker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import cryptobot.AppConfig;
import cryptobot.misc.MailSender;

	@Value("${cryptobot.currencyPair}") protected CurrencyPair ccyp;
	@Value("${cryptobot.price.precision}") protected int scale;
	@Value("${cryptobot.volume.precision}") protected int volScale;
	protected BaseExchange xchange;
	protected final ConcurrentHashMap<Currency, BalanceInfo> balanceInfoMap = new ConcurrentHashMap<>();
	protected final OrderPool orderPool = new OrderPool();
	protected volatile Ticker lastTick;
	protected final Logger log = LoggerFactory.getLogger(this.getClass());
	private boolean enable = true;
	private boolean allowBid = true;
	private boolean allowAsk = true;
	@Autowired ApplicationEventPublisher publisher;
	@Autowired protected AppConfig config;
	@Autowired protected RestClient restClient;
	@Autowired MailSender mailer;
	protected Fee fee = new Fee(ZERO, ZERO);
	public static enum FeeType
	@Autowired TickerLogger tickerLogger;
		final Set<String> expectedOrderId = new HashSet<>();

		synchronized public OrderRecord waitOrder(String orderId, List<OrderStatus> status) throws InterruptedException {
			var startTime = System.currentTimeMillis();
			while (System.currentTimeMillis() - startTime < 10*1000L) {
				var order = get(orderId);
				if (order != null) {
					if (status == null) {
						return order;
					} else if (status.contains(order.getStatus())) {
						return order;
					}
				}
				expectedOrderId.add(orderId);
				this.wait(3000L);
				expectedOrderId.remove(orderId);
			}
			return null;
		}

```
### /Users/kh/git/ccy/cryptobot/src/main/java/cryptobot/exchange/ExchangeService.java

```java
import static java.math.BigDecimal.*;
import static java.math.RoundingMode.*;
import static java.util.Optional.*;
import static java.util.stream.Collectors.*;
import static org.knowm.xchange.dto.Order.OrderStatus.*;
import static org.knowm.xchange.dto.Order.OrderType.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import cryptobot.dto.*;
import org.springframework.web.client.RestClient;
import org.knowm.xchange.BaseExchange;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.account.Balance;
import org.knowm.xchange.dto.account.Fee;
import org.knowm.xchange.dto.marketdata.Ticker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import cryptobot.AppConfig;
import cryptobot.misc.MailSender;

	@Value("${cryptobot.currencyPair}") protected CurrencyPair ccyp;
	@Value("${cryptobot.price.precision}") protected int scale;
	@Value("${cryptobot.volume.precision}") protected int volScale;
	protected BaseExchange xchange;
	protected final ConcurrentHashMap<Currency, BalanceInfo> balanceInfoMap = new ConcurrentHashMap<>();
	protected final OrderPool orderPool = new OrderPool();
	protected volatile Ticker lastTick;
	protected final Logger log = LoggerFactory.getLogger(this.getClass());
	private boolean enable = true;
	private boolean allowBid = true;
	private boolean allowAsk = true;
	@Autowired ApplicationEventPublisher publisher;
	@Autowired protected AppConfig config;
	@Autowired protected RestClient restClient;
	@Autowired MailSender mailer;
	protected Fee fee = new Fee(ZERO, ZERO);
	public static enum FeeType
	@Autowired TickerLogger tickerLogger;
		final Set<String> expectedOrderId = new HashSet<>();

		synchronized public void update(OrderRecord order) {
			var current = get(order.getId());
			if (current == null && order.getUserReference() != null) {
				current = get(order.getUserReference());
				// idがなくてrefだけ存在する場合（id未確定で注文受付直後の状態,bitflyerでchild_order_acceptance_idのみある状態）
				if (current != null) {
					current = order;
					put(order.getId(), order);
					put(order.getUserReference(), order);
				}
			}
			if (current == null) {
				super.put(order.getId(), order);
				if (order.getUserReference() != null)
					super.put(order.getUserReference(), order);
				if (this.size() > 200) {
					var op = values().stream().filter(o -> o.getStatus().equals(CANCELED)).findFirst();
					if (op.isPresent() == false)
						op = values().stream().filter(o -> !o.isActive()).findFirst();
					op.ifPresent(o -> {
						this.remove(o.getId());
						if (o.getUserReference() != null)
							this.remove(o.getUserReference());
					});
				}
			}
			else {
				current.updateBy(order);
			}
			if (expectedOrderId.contains(order.getId())) {
				expectedOrderId.remove(order.getId());
				notifyAll();
			}
			if (expectedOrderId.contains(order.getUserReference())) {
				expectedOrderId.remove(order.getUserReference());
				notifyAll();
			}
		}

```
### /Users/kh/git/ccy/cryptobot/src/main/java/cryptobot/exchange/bitbank/Bitbank.java

```java
import static cryptobot.exchange.ExchangeService.FeeType.*;
import static java.lang.String.*;
import static java.math.BigDecimal.*;
import static java.math.RoundingMode.*;
import static java.util.Objects.*;
import static java.util.stream.Collectors.*;
import static org.apache.commons.lang3.StringUtils.*;
import static org.knowm.xchange.dto.Order.OrderStatus.*;
import static org.knowm.xchange.dto.Order.OrderType.*;
import java.math.BigDecimal;
import java.util.Map;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import cryptobot.dto.OrderRecord;
import cryptobot.exchange.OrderbookService;
import cryptobot.exchange.bitbank.dto.*;
import cryptobot.misc.MailSender;

	@Value("${exchange.api.bitbank:https://api.bitbank.cc}")
	private String API_ENDPOINT;
	@Value("${bitbank.web.2fa.secret}") String SECRET_KEY_FOR_2FA;
	@Value("${bitbank.api.key}") String API_KEY;
	@Value("${bitbank.api.secret}") String SECRET;
	@Value("${bitbank.fee.withdraw.counter}") BigDecimal COUNTER_WITHDRAW_FEE;
	@Value("${bitbank.fee.withdraw.base}") BigDecimal BASE_WITHDRAW_FEE;
	@Value("${bitbank.address.eth:#{null}}") String ETH_ADDRESS;
	@Value("${bitbank.address.bch:#{null}}") String BCH_ADDRESS;
	@Value("${bitbank.address.btc:#{null}}") String BTC_ADDRESS;
	@Value("${bitbank.rebalance.threshold.base:0}") BigDecimal rebalanceThresholdBase;
	@Value("${bitbank.rebalance.target.base:0}") BigDecimal rebalanceTargetBase;
	@Value("${bitbank.rebalance.threshold.counter:0}") BigDecimal rebalanceThresholdCounter;
	@Value("${bitbank.rebalance.target.counter:0}") BigDecimal rebalanceTargetCounter;
	@Value("${cryptobot.healthcheck}") private String enableHealthCheck;
	@Autowired MailSender mail;
	@Autowired BitbankOrderbookService orderbookService;
	BitbankAssets assetsResponse = null;
	String ccyps;
	private Map<String, String> errorCodes;
	static Map<CurrencyPair, String> SUPPORTED_CCYPS = Map.of(
			CurrencyPair.BTC_JPY, "BTC_JPY"
	);

	@Override
	synchronized public OrderRecord buySpot(BigDecimal volume, BigDecimal limitPrice) throws Exception {
		return executeOrder(volume, limitPrice, BitbankOrders.Side.BUY, BitbankOrders.Type.LIMIT);
	}

```
### /Users/kh/git/ccy/cryptobot/src/main/java/cryptobot/exchange/bitbank/Bitbank.java

```java
import static cryptobot.exchange.ExchangeService.FeeType.*;
import static java.lang.String.*;
import static java.lang.System.currentTimeMillis;
import static java.math.BigDecimal.*;
import static java.math.RoundingMode.*;
import static java.util.Objects.*;
import static java.util.stream.Collectors.*;
import static org.apache.commons.lang3.StringUtils.*;
import static org.knowm.xchange.dto.Order.OrderStatus.*;
import static org.knowm.xchange.dto.Order.OrderType.*;
import java.math.BigDecimal;
import java.util.Map;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import cryptobot.dto.OrderRecord;
import cryptobot.exchange.OrderNotFilledException;
import cryptobot.exchange.OrderbookService;
import cryptobot.exchange.bitbank.dto.*;
import cryptobot.misc.MailSender;

	@Value("${exchange.api.bitbank:https://api.bitbank.cc}")
	private String API_ENDPOINT;
	@Value("${bitbank.web.2fa.secret}") String SECRET_KEY_FOR_2FA;
	@Value("${bitbank.api.key}") String API_KEY;
	@Value("${bitbank.api.secret}") String SECRET;
	@Value("${bitbank.fee.withdraw.counter}") BigDecimal COUNTER_WITHDRAW_FEE;
	@Value("${bitbank.fee.withdraw.base}") BigDecimal BASE_WITHDRAW_FEE;
	@Value("${bitbank.address.eth:#{null}}") String ETH_ADDRESS;
	@Value("${bitbank.address.bch:#{null}}") String BCH_ADDRESS;
	@Value("${bitbank.address.btc:#{null}}") String BTC_ADDRESS;
	@Value("${bitbank.rebalance.threshold.base:0}") BigDecimal rebalanceThresholdBase;
	@Value("${bitbank.rebalance.target.base:0}") BigDecimal rebalanceTargetBase;
	@Value("${bitbank.rebalance.threshold.counter:0}") BigDecimal rebalanceThresholdCounter;
	@Value("${bitbank.rebalance.target.counter:0}") BigDecimal rebalanceTargetCounter;
	@Value("${cryptobot.healthcheck}") private String enableHealthCheck;
	@Autowired MailSender mail;
	@Autowired BitbankOrderbookService orderbookService;
	BitbankAssets assetsResponse = null;
	String ccyps;
	private Map<String, String> errorCodes;
	static Map<CurrencyPair, String> SUPPORTED_CCYPS = Map.of(
			CurrencyPair.BTC_JPY, "BTC_JPY"
	);

	private OrderRecord executeOrder(BigDecimal volume, BigDecimal price, BitbankOrders.Side side, BitbankOrders.Type type) throws Exception {
		var path = "/v1/user/spot/order";
		var body = BitbankOrderBody.builder()
				.pair(ccyps)
				.amount(volume.toString())
				.price(price)
				.side(side.getCode())
				.type(type.getCode())
				.build();
		
		var order = doHttpPost(path, BitbankOrders.Order.class, body);

		log.info("order sent. id: {}", order.orderId);
		var orderId = String.valueOf(order.orderId);
		
		if (type == BitbankOrders.Type.MARKET) {
			var startTime = currentTimeMillis();
			while (currentTimeMillis() - startTime < 3600 * 1000L) {
				OrderRecord or = waitOrderClosed(orderId);
				if (or != null && !or.isActive()) return or;
			}
			throw new OrderNotFilledException(orderPool.getById(orderId));
		} else {
			return waitOrderCreate(orderId);
		}
	}

```
### /Users/kh/git/ccy/cryptobot/src/main/java/cryptobot/exchange/bitbank/Bitbank.java

```java
import static cryptobot.exchange.ExchangeService.FeeType.*;
import static java.lang.String.*;
import static java.math.BigDecimal.*;
import static java.math.RoundingMode.*;
import static java.util.Objects.*;
import static java.util.stream.Collectors.*;
import static org.apache.commons.lang3.StringUtils.*;
import static org.knowm.xchange.dto.Order.OrderStatus.*;
import static org.knowm.xchange.dto.Order.OrderType.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import java.math.BigDecimal;
import java.util.Map;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.JsonNode;
import cryptobot.exchange.OrderbookService;
import cryptobot.exchange.bitbank.dto.*;
import cryptobot.misc.JsonUtils;
import cryptobot.misc.MailSender;

	@Value("${exchange.api.bitbank:https://api.bitbank.cc}")
	private String API_ENDPOINT;
	@Value("${bitbank.web.2fa.secret}") String SECRET_KEY_FOR_2FA;
	@Value("${bitbank.api.key}") String API_KEY;
	@Value("${bitbank.api.secret}") String SECRET;
	@Value("${bitbank.fee.withdraw.counter}") BigDecimal COUNTER_WITHDRAW_FEE;
	@Value("${bitbank.fee.withdraw.base}") BigDecimal BASE_WITHDRAW_FEE;
	@Value("${bitbank.address.eth:#{null}}") String ETH_ADDRESS;
	@Value("${bitbank.address.bch:#{null}}") String BCH_ADDRESS;
	@Value("${bitbank.address.btc:#{null}}") String BTC_ADDRESS;
	@Value("${bitbank.rebalance.threshold.base:0}") BigDecimal rebalanceThresholdBase;
	@Value("${bitbank.rebalance.target.base:0}") BigDecimal rebalanceTargetBase;
	@Value("${bitbank.rebalance.threshold.counter:0}") BigDecimal rebalanceThresholdCounter;
	@Value("${bitbank.rebalance.target.counter:0}") BigDecimal rebalanceTargetCounter;
	@Value("${cryptobot.healthcheck}") private String enableHealthCheck;
	@Autowired MailSender mail;
	@Autowired BitbankOrderbookService orderbookService;
	BitbankAssets assetsResponse = null;
	String ccyps;
	private Map<String, String> errorCodes;
	static Map<CurrencyPair, String> SUPPORTED_CCYPS = Map.of(
			CurrencyPair.BTC_JPY, "BTC_JPY"
	);

	protected <T> T doHttpPost(String path, Class<T> clazz, Object body) throws Exception {
		String json = JsonUtils.MAPPER.writeValueAsString(body);
		Map<String, String> headers = getPrivateRequestHeader(json);

		String res = restClient.post()
				.uri(API_ENDPOINT + path)
				.headers(h -> headers.forEach(h::add))
				.contentType(APPLICATION_JSON)
				.body(json)
				.retrieve()
				.body(String.class);
		JsonNode data = JsonUtils.MAPPER.readTree(res).path("data");
		return data.traverse(JsonUtils.MAPPER).readValueAs(clazz);
	}

```
### /Users/kh/git/ccy/cryptobot/src/main/java/cryptobot/dto/OrderRecord.java

```java
import cryptobot.exchange.ExchangeService;
import lombok.ToString;
import org.knowm.xchange.currency.Currency;
import java.util.*;
import static org.knowm.xchange.dto.Order.OrderStatus.*;

	private ExchangeService service;
	private HashMap<String, Object> metaInfo = new HashMap<>();
	@ToString.Exclude private Exception exception;
	private Currency feeCurrency;

	public boolean isActive() {
		return getStatus() == null || !getStatus().isFinal();
	}

```
### /Users/kh/git/ccy/cryptobot/src/main/java/cryptobot/exchange/ExchangeService.java

```java
import static java.math.BigDecimal.*;
import static java.math.RoundingMode.*;
import static java.util.Optional.*;
import static java.util.stream.Collectors.*;
import static org.knowm.xchange.dto.Order.OrderStatus.*;
import static org.knowm.xchange.dto.Order.OrderType.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import cryptobot.dto.*;
import org.springframework.web.client.RestClient;
import org.knowm.xchange.BaseExchange;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.Order.OrderType;
import org.knowm.xchange.dto.account.Balance;
import org.knowm.xchange.dto.account.Fee;
import org.knowm.xchange.dto.marketdata.Ticker;
import org.knowm.xchange.dto.marketdata.Trade;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.knowm.xchange.service.trade.TradeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import cryptobot.AppConfig;
import cryptobot.misc.MailSender;

	@Value("${cryptobot.currencyPair}") protected CurrencyPair ccyp;
	@Value("${cryptobot.price.precision}") protected int scale;
	@Value("${cryptobot.volume.precision}") protected int volScale;
	protected BaseExchange xchange;
	protected final ConcurrentHashMap<Currency, BalanceInfo> balanceInfoMap = new ConcurrentHashMap<>();
	protected final OrderPool orderPool = new OrderPool();
	protected volatile Ticker lastTick;
	protected final Logger log = LoggerFactory.getLogger(this.getClass());
	private boolean enable = true;
	private boolean allowBid = true;
	private boolean allowAsk = true;
	@Autowired ApplicationEventPublisher publisher;
	@Autowired protected AppConfig config;
	@Autowired protected RestClient restClient;
	@Autowired MailSender mailer;
	protected Fee fee = new Fee(ZERO, ZERO);
	public static enum FeeType
	@Autowired TickerLogger tickerLogger;
		final Set<String> expectedOrderId = new HashSet<>();

	public OrderRecord buySpot(BigDecimal volume, BigDecimal limitPrice) throws Exception {
		LimitOrder order = new LimitOrder(OrderType.BID, volume, ccyp, null, null, limitPrice);
		String id = xchange.getTradeService().placeLimitOrder(order);
		return getOrder(id);
	}

```
### /Users/kh/git/ccy/cryptobot/src/main/java/cryptobot/exchange/bitfinex/Bitfinex.java

```java
import static cryptobot.exchange.ExchangeService.FeeType.*;
import static java.math.BigDecimal.*;
import static java.math.RoundingMode.*;
import static java.util.Objects.*;
import static java.util.stream.Collectors.*;
import static org.knowm.xchange.dto.Order.OrderStatus.*;
import static org.knowm.xchange.dto.Order.OrderType.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import org.knowm.xchange.bitfinex.service.BitfinexTradeService;
import org.knowm.xchange.bitfinex.v1.BitfinexOrderType;
import org.knowm.xchange.bitfinex.v1.dto.trade.BitfinexOrderStatusResponse;
import org.knowm.xchange.dto.Order.OrderStatus;
import org.knowm.xchange.dto.Order.OrderType;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import cryptobot.dto.OrderRecord;
import cryptobot.exchange.OrderbookService;

	private static final BigDecimal TRADING_CHARGE = new BigDecimal("0.00200000");
	private BitfinexTradeService tradeService;
	@Value("${bitfinex.api.key}") private String API_KEY;
	@Value("${bitfinex.api.secret}") private String SECRET;
	@Value("${bitfinex.fee.withdraw.counter}") BigDecimal COUNTER_WITHDRAW_FEE;
	@Value("${bitfinex.fee.withdraw.base}") BigDecimal BASE_WITHDRAW_FEE;
	@Value("${bitfinex.address.bch:#{null}}") String BCH_ADDRESS;
	@Value("${bitfinex.address.xlm:#{null}}") String XLM_ADDRESS;
	@Value("${bitfinex.address.xlm.memo:#{null}}") String XLM_MEMO;
	@Value("${bitfinex.rebalance.threshold.base:0}") BigDecimal rebalanceThresholdBase;
	@Value("${bitfinex.rebalance.target.base:0}") BigDecimal rebalanceTargetBase;
	@Value("${bitfinex.rebalance.threshold.counter:0}") BigDecimal rebalanceThresholdCounter;
	@Value("${bitfinex.rebalance.target.counter:0}") BigDecimal rebalanceTargetCounter;
	@Autowired BitfinexOrderbookService orderbookService;

	@Override
	public OrderRecord buySpot(BigDecimal volume, BigDecimal limitPrice) throws Exception {
		// 取得通貨で手数料を徴収されるため、取引金額(volume)に手数分を上乗せて受け渡す。
		volume = volume.divide(ONE.subtract(getFeeRate(MAKER)),
				8, RoundingMode.HALF_UP);
		LimitOrder limitOrder = new LimitOrder.Builder(OrderType.BID, ccyp)
				.limitPrice(limitPrice).originalAmount(volume).build();
		BitfinexOrderStatusResponse newOrder = tradeService.placeBitfinexLimitOrder(
				limitOrder, BitfinexOrderType.LIMIT);
		log.info("order executed. id: {}", newOrder);
		return waitOrderCreate(String.valueOf(newOrder.getId()));

	}

```
### /Users/kh/git/ccy/cryptobot/src/main/java/cryptobot/exchange/bitflyer/Bitflyer.java

```java
import com.fasterxml.jackson.databind.ObjectMapper;
import cryptobot.dto.HealthStatus;
import cryptobot.dto.OrderRecord;
import cryptobot.exchange.OrderbookService;
import cryptobot.misc.JsonUtils;
import cryptobot.misc.MailSender;
import cryptobot.misc.Util;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order.OrderType;
import org.knowm.xchange.dto.account.Fee;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import static java.math.BigDecimal.ONE;
import static java.math.RoundingMode.HALF_UP;
import static org.knowm.xchange.dto.Order.OrderType.BID;

	@Value("${exchange.api.bitflyer:https://api.bitflyer.jp}")
	private String URL_BASE;
	private static final String APPLICATION_JSON = "application/json";
	private static final String ACCESS_KEY = "ACCESS-KEY";
	private static final String ACCESS_TIMESTAMP = "ACCESS-TIMESTAMP";
	private static final String ACCESS_SIGN = "ACCESS-SIGN";
	private static final String CONTENT_TYPE = "Content-Type";
	@Value("${bitflyer.api.key:#{null}}") String API_KEY;
	@Value("${bitflyer.api.secret:#{null}}") String SECRET;
	@Value("${bitflyer.fee.withdraw.counter:#{null}}") BigDecimal COUNTER_WITHDRAW_FEE;
	@Value("${bitflyer.fee.withdraw.base:#{null}}") BigDecimal BASE_WITHDRAW_FEE;
	@Value("${bitflyer.rebalance.threshold.base:0}") BigDecimal rebalanceThresholdBase;
	@Value("${bitflyer.rebalance.target.base:0}") BigDecimal rebalanceTargetBase;
	@Value("${bitflyer.rebalance.threshold.counter:0}") BigDecimal rebalanceThresholdCounter;
	@Value("${bitflyer.rebalance.target.counter:0}") BigDecimal rebalanceTargetCounter;
	@Value("${cryptobot.healthcheck}") private String enableHealthCheck;
	private HealthStatus status;
	@Autowired MailSender mail;
	@Autowired BitflyerOrderbookService orderbookService;
	@Autowired BitflyerWeb web;
	ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(
			Thread.ofVirtual().name("bf_healthcheck").factory());
	private static final ObjectMapper OBJECT_MAPPER = JsonUtils.MAPPER;
	public static enum OrderFilterBy
	public enum BitflyerOrderType
		private static final Map<String, BitflyerOrderType> fromString = new HashMap<String, BitflyerOrderType>();
	static Map<CurrencyPair, String> SUPPORTED_CCYPS = Map.of(
		CurrencyPair.BTC_JPY, "BTC_JPY",
		CurrencyPair.ETH_BTC, "ETH_BTC",
		CurrencyPair.BCH_BTC, "BCH_BTC");

	@Override
	public OrderRecord buySpot(BigDecimal volume, BigDecimal limitPrice) throws Exception {
		// 取引通貨(base currency)で手数料を徴収されるため、徴収後の取得額が引数で指定された額になるよう調整
		volume = volume.divide(ONE.subtract(getFeeRate(FeeType.MAKER)), volScale, HALF_UP);
		LimitOrder order = new LimitOrder.Builder(OrderType.BID, ccyp)
				.limitPrice(limitPrice).originalAmount(volume).build();
		String acceptanceId = placeLimitOrder(order);
		log.info("order sent. id: {}", acceptanceId);
		return waitOrderCreate(acceptanceId);
	}

```
### /Users/kh/git/ccy/cryptobot/src/main/java/cryptobot/exchange/bitflyer/Bitflyer.java

```java
import com.fasterxml.jackson.databind.ObjectMapper;
import cryptobot.dto.HealthStatus;
import cryptobot.exchange.OrderbookService;
import cryptobot.exchange.bitflyer.dto.BitflyerNewOrderRequest;
import cryptobot.exchange.bitflyer.dto.OrderResult;
import cryptobot.misc.JsonUtils;
import cryptobot.misc.MailSender;
import cryptobot.misc.Util;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order.OrderType;
import org.knowm.xchange.dto.trade.MarketOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

	@Value("${exchange.api.bitflyer:https://api.bitflyer.jp}")
	private String URL_BASE;
	private static final String APPLICATION_JSON = "application/json";
	private static final String ACCESS_KEY = "ACCESS-KEY";
	private static final String ACCESS_TIMESTAMP = "ACCESS-TIMESTAMP";
	private static final String ACCESS_SIGN = "ACCESS-SIGN";
	private static final String CONTENT_TYPE = "Content-Type";
	@Value("${bitflyer.api.key:#{null}}") String API_KEY;
	@Value("${bitflyer.api.secret:#{null}}") String SECRET;
	@Value("${bitflyer.fee.withdraw.counter:#{null}}") BigDecimal COUNTER_WITHDRAW_FEE;
	@Value("${bitflyer.fee.withdraw.base:#{null}}") BigDecimal BASE_WITHDRAW_FEE;
	@Value("${bitflyer.rebalance.threshold.base:0}") BigDecimal rebalanceThresholdBase;
	@Value("${bitflyer.rebalance.target.base:0}") BigDecimal rebalanceTargetBase;
	@Value("${bitflyer.rebalance.threshold.counter:0}") BigDecimal rebalanceThresholdCounter;
	@Value("${bitflyer.rebalance.target.counter:0}") BigDecimal rebalanceTargetCounter;
	@Value("${cryptobot.healthcheck}") private String enableHealthCheck;
	private HealthStatus status;
	@Autowired MailSender mail;
	@Autowired BitflyerOrderbookService orderbookService;
	@Autowired BitflyerWeb web;
	ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(
			Thread.ofVirtual().name("bf_healthcheck").factory());
	private static final ObjectMapper OBJECT_MAPPER = JsonUtils.MAPPER;
	public static enum OrderFilterBy
	public enum BitflyerOrderType
		private static final Map<String, BitflyerOrderType> fromString = new HashMap<String, BitflyerOrderType>();
	static Map<CurrencyPair, String> SUPPORTED_CCYPS = Map.of(
		CurrencyPair.BTC_JPY, "BTC_JPY",
		CurrencyPair.ETH_BTC, "ETH_BTC",
		CurrencyPair.BCH_BTC, "BCH_BTC");

	public String placeMarketOrder(MarketOrder order) throws Exception {
		String type = BitflyerOrderType.fromOrderType(order.getType()).toString();
		String product = toPairString(order.getCurrencyPair());
		String path = "/v1/me/sendchildorder";
		BitflyerNewOrderRequest request = new BitflyerNewOrderRequest(
				"MARKET", product, type, order.getOriginalAmount(), order.getAveragePrice());
		String requestBody = OBJECT_MAPPER.writeValueAsString(request);
		try {
			OrderResult orderResult = doPostWithAuth(path, requestBody, OrderResult.class);
			if (orderResult==null) return null;
        	return orderResult.getChildOrderAcceptanceId();
		} catch (Exception e) {
			throw e;
		}
	}

```
### /Users/kh/git/ccy/cryptobot/src/main/java/cryptobot/exchange/bitflyer/Bitflyer.java

```java
import com.fasterxml.jackson.databind.ObjectMapper;
import cryptobot.dto.HealthStatus;
import cryptobot.exchange.OrderbookService;
import cryptobot.misc.JsonUtils;
import cryptobot.misc.MailSender;
import cryptobot.misc.Util;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order.OrderType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

	@Value("${exchange.api.bitflyer:https://api.bitflyer.jp}")
	private String URL_BASE;
	private static final String APPLICATION_JSON = "application/json";
	private static final String ACCESS_KEY = "ACCESS-KEY";
	private static final String ACCESS_TIMESTAMP = "ACCESS-TIMESTAMP";
	private static final String ACCESS_SIGN = "ACCESS-SIGN";
	private static final String CONTENT_TYPE = "Content-Type";
	@Value("${bitflyer.api.key:#{null}}") String API_KEY;
	@Value("${bitflyer.api.secret:#{null}}") String SECRET;
	@Value("${bitflyer.fee.withdraw.counter:#{null}}") BigDecimal COUNTER_WITHDRAW_FEE;
	@Value("${bitflyer.fee.withdraw.base:#{null}}") BigDecimal BASE_WITHDRAW_FEE;
	@Value("${bitflyer.rebalance.threshold.base:0}") BigDecimal rebalanceThresholdBase;
	@Value("${bitflyer.rebalance.target.base:0}") BigDecimal rebalanceTargetBase;
	@Value("${bitflyer.rebalance.threshold.counter:0}") BigDecimal rebalanceThresholdCounter;
	@Value("${bitflyer.rebalance.target.counter:0}") BigDecimal rebalanceTargetCounter;
	@Value("${cryptobot.healthcheck}") private String enableHealthCheck;
	private HealthStatus status;
	@Autowired MailSender mail;
	@Autowired BitflyerOrderbookService orderbookService;
	@Autowired BitflyerWeb web;
	ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(
			Thread.ofVirtual().name("bf_healthcheck").factory());
	private static final ObjectMapper OBJECT_MAPPER = JsonUtils.MAPPER;
	public static enum OrderFilterBy
	public enum BitflyerOrderType
		private static final Map<String, BitflyerOrderType> fromString = new HashMap<String, BitflyerOrderType>();
	static Map<CurrencyPair, String> SUPPORTED_CCYPS = Map.of(
		CurrencyPair.BTC_JPY, "BTC_JPY",
		CurrencyPair.ETH_BTC, "ETH_BTC",
		CurrencyPair.BCH_BTC, "BCH_BTC");

	private <T> T doPostWithAuth(String path, String requestBody, Class<T> responseClass) {
		String timestamp = String.valueOf(new Date().getTime());
		String data = timestamp + "POST" + path + requestBody;
		String hash = createHMAC(SECRET, data);
		return restClient.post()
				.uri(URL_BASE + path)
				.header(ACCESS_KEY, API_KEY)
				.header(ACCESS_TIMESTAMP, timestamp)
				.header(ACCESS_SIGN, hash)
				.contentType(MediaType.APPLICATION_JSON)
				.body(requestBody)
				.retrieve()
				.body(responseClass);
	}

```
### /Users/kh/git/ccy/cryptobot/src/main/java/cryptobot/exchange/bitflyer/dto/OrderResult.java

```java
import com.fasterxml.jackson.annotation.JsonProperty;

	@JsonProperty("child_order_acceptance_id")
	private String childOrderAcceptanceId;

	public String getChildOrderAcceptanceId() {
		return childOrderAcceptanceId;
	}

```
### /Users/kh/git/ccy/cryptobot/src/main/java/cryptobot/exchange/bitflyer/Bitflyer.java

```java
import com.fasterxml.jackson.databind.ObjectMapper;
import cryptobot.dto.HealthStatus;
import cryptobot.dto.OrderRecord;
import cryptobot.exchange.OrderbookService;
import cryptobot.misc.JsonUtils;
import cryptobot.misc.MailSender;
import cryptobot.misc.Util;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order.OrderType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

	@Value("${exchange.api.bitflyer:https://api.bitflyer.jp}")
	private String URL_BASE;
	private static final String APPLICATION_JSON = "application/json";
	private static final String ACCESS_KEY = "ACCESS-KEY";
	private static final String ACCESS_TIMESTAMP = "ACCESS-TIMESTAMP";
	private static final String ACCESS_SIGN = "ACCESS-SIGN";
	private static final String CONTENT_TYPE = "Content-Type";
	@Value("${bitflyer.api.key:#{null}}") String API_KEY;
	@Value("${bitflyer.api.secret:#{null}}") String SECRET;
	@Value("${bitflyer.fee.withdraw.counter:#{null}}") BigDecimal COUNTER_WITHDRAW_FEE;
	@Value("${bitflyer.fee.withdraw.base:#{null}}") BigDecimal BASE_WITHDRAW_FEE;
	@Value("${bitflyer.rebalance.threshold.base:0}") BigDecimal rebalanceThresholdBase;
	@Value("${bitflyer.rebalance.target.base:0}") BigDecimal rebalanceTargetBase;
	@Value("${bitflyer.rebalance.threshold.counter:0}") BigDecimal rebalanceThresholdCounter;
	@Value("${bitflyer.rebalance.target.counter:0}") BigDecimal rebalanceTargetCounter;
	@Value("${cryptobot.healthcheck}") private String enableHealthCheck;
	private HealthStatus status;
	@Autowired MailSender mail;
	@Autowired BitflyerOrderbookService orderbookService;
	@Autowired BitflyerWeb web;
	ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(
			Thread.ofVirtual().name("bf_healthcheck").factory());
	private static final ObjectMapper OBJECT_MAPPER = JsonUtils.MAPPER;
	public static enum OrderFilterBy
	public enum BitflyerOrderType
		private static final Map<String, BitflyerOrderType> fromString = new HashMap<String, BitflyerOrderType>();
	static Map<CurrencyPair, String> SUPPORTED_CCYPS = Map.of(
		CurrencyPair.BTC_JPY, "BTC_JPY",
		CurrencyPair.ETH_BTC, "ETH_BTC",
		CurrencyPair.BCH_BTC, "BCH_BTC");

	/**
	 * 成行注文の約定待ち合わせ
	 * @param acceptanceId CHILD_ORDER_ACCEPTANCE_ID(注文送信直後なのでCHILD_ORDER_IDが未決定の状態を想定)
	 * @return
	 * @throws Exception
	 */
	@Override
	public OrderRecord waitOrderClosed(String acceptanceId) throws Exception {
		// 希にbitFlyerで成行注文の約定に時間がかかる場合があるため、約定を待機
		return retry(3, 60*1000, ()->{
			// リアルタイムAPIでの状態更新を待機
			var order = orderPool.waitOrderClosed(acceptanceId);
			// 状態更新を確認できた場合
			if (order != null) {
				return order;
			}
			// 状態更新のタイムアウトの場合
			else {
				var result = getOrderWithRetry(acceptanceId);
				if(result.isActive()) {
					throw new Exception("market order not settled.");
				}
				return result;
			}
		});
	}

```
### /Users/kh/git/ccy/cryptobot/src/main/java/cryptobot/exchange/GenericService.java

```java
import static java.math.BigDecimal.*;
import static org.knowm.xchange.dto.Order.OrderStatus.*;
import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

	protected final Logger log = LoggerFactory.getLogger(getClass());
	protected volatile boolean serviceStarted = false;

	protected<T> T retry(int count, long wait, Callable<T> callable) throws Exception {
		int n = 0;
		while (true) {
			try {
				return callable.call();
			} catch (Exception e) {
				if (++n > count) throw e;
				log.warn("retry will be executed - caused by : {}", e.getMessage());
				Thread.sleep(wait);
			}
		}
	}

```
### /Users/kh/git/ccy/cryptobot/src/main/java/cryptobot/exchange/bitflyer/Bitflyer.java

```java
import com.fasterxml.jackson.databind.ObjectMapper;
import cryptobot.dto.HealthStatus;
import cryptobot.dto.OrderRecord;
import cryptobot.exchange.OrderbookService;
import cryptobot.exchange.PlacedOrderNotFoundException;
import cryptobot.misc.JsonUtils;
import cryptobot.misc.MailSender;
import cryptobot.misc.Util;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order.OrderType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import static cryptobot.exchange.bitflyer.Bitflyer.OrderFilterBy.child_order_acceptance_id;

	@Value("${exchange.api.bitflyer:https://api.bitflyer.jp}")
	private String URL_BASE;
	private static final String APPLICATION_JSON = "application/json";
	private static final String ACCESS_KEY = "ACCESS-KEY";
	private static final String ACCESS_TIMESTAMP = "ACCESS-TIMESTAMP";
	private static final String ACCESS_SIGN = "ACCESS-SIGN";
	private static final String CONTENT_TYPE = "Content-Type";
	@Value("${bitflyer.api.key:#{null}}") String API_KEY;
	@Value("${bitflyer.api.secret:#{null}}") String SECRET;
	@Value("${bitflyer.fee.withdraw.counter:#{null}}") BigDecimal COUNTER_WITHDRAW_FEE;
	@Value("${bitflyer.fee.withdraw.base:#{null}}") BigDecimal BASE_WITHDRAW_FEE;
	@Value("${bitflyer.rebalance.threshold.base:0}") BigDecimal rebalanceThresholdBase;
	@Value("${bitflyer.rebalance.target.base:0}") BigDecimal rebalanceTargetBase;
	@Value("${bitflyer.rebalance.threshold.counter:0}") BigDecimal rebalanceThresholdCounter;
	@Value("${bitflyer.rebalance.target.counter:0}") BigDecimal rebalanceTargetCounter;
	@Value("${cryptobot.healthcheck}") private String enableHealthCheck;
	private HealthStatus status;
	@Autowired MailSender mail;
	@Autowired BitflyerOrderbookService orderbookService;
	@Autowired BitflyerWeb web;
	ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(
			Thread.ofVirtual().name("bf_healthcheck").factory());
	private static final ObjectMapper OBJECT_MAPPER = JsonUtils.MAPPER;
	public static enum OrderFilterBy
	public enum BitflyerOrderType
		private static final Map<String, BitflyerOrderType> fromString = new HashMap<String, BitflyerOrderType>();
	static Map<CurrencyPair, String> SUPPORTED_CCYPS = Map.of(
		CurrencyPair.BTC_JPY, "BTC_JPY",
		CurrencyPair.ETH_BTC, "ETH_BTC",
		CurrencyPair.BCH_BTC, "BCH_BTC");

	private OrderRecord getOrderWithRetry(String acceptanceId) throws Exception {
		for (int i = 0; i < 30; i++) {
			try {
				var orderRecord = getOrder(acceptanceId, child_order_acceptance_id);
				if (orderRecord != null) return orderRecord;
			} catch (Exception e) {
				log.warn(e.getMessage());
			}
			Thread.sleep(3*1000);
		}
		throw new PlacedOrderNotFoundException(acceptanceId);
	}

```
### /Users/kh/git/ccy/cryptobot/src/main/java/cryptobot/exchange/bitflyer/Bitflyer.java

```java
import com.fasterxml.jackson.databind.ObjectMapper;
import cryptobot.dto.HealthStatus;
import cryptobot.exchange.OrderbookService;
import cryptobot.exchange.bitflyer.dto.BitflyerNewOrderRequest;
import cryptobot.exchange.bitflyer.dto.OrderResult;
import cryptobot.misc.JsonUtils;
import cryptobot.misc.MailSender;
import cryptobot.misc.Util;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order.OrderType;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

	@Value("${exchange.api.bitflyer:https://api.bitflyer.jp}")
	private String URL_BASE;
	private static final String APPLICATION_JSON = "application/json";
	private static final String ACCESS_KEY = "ACCESS-KEY";
	private static final String ACCESS_TIMESTAMP = "ACCESS-TIMESTAMP";
	private static final String ACCESS_SIGN = "ACCESS-SIGN";
	private static final String CONTENT_TYPE = "Content-Type";
	@Value("${bitflyer.api.key:#{null}}") String API_KEY;
	@Value("${bitflyer.api.secret:#{null}}") String SECRET;
	@Value("${bitflyer.fee.withdraw.counter:#{null}}") BigDecimal COUNTER_WITHDRAW_FEE;
	@Value("${bitflyer.fee.withdraw.base:#{null}}") BigDecimal BASE_WITHDRAW_FEE;
	@Value("${bitflyer.rebalance.threshold.base:0}") BigDecimal rebalanceThresholdBase;
	@Value("${bitflyer.rebalance.target.base:0}") BigDecimal rebalanceTargetBase;
	@Value("${bitflyer.rebalance.threshold.counter:0}") BigDecimal rebalanceThresholdCounter;
	@Value("${bitflyer.rebalance.target.counter:0}") BigDecimal rebalanceTargetCounter;
	@Value("${cryptobot.healthcheck}") private String enableHealthCheck;
	private HealthStatus status;
	@Autowired MailSender mail;
	@Autowired BitflyerOrderbookService orderbookService;
	@Autowired BitflyerWeb web;
	ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(
			Thread.ofVirtual().name("bf_healthcheck").factory());
	private static final ObjectMapper OBJECT_MAPPER = JsonUtils.MAPPER;
	public static enum OrderFilterBy
	public enum BitflyerOrderType
		private static final Map<String, BitflyerOrderType> fromString = new HashMap<String, BitflyerOrderType>();
	static Map<CurrencyPair, String> SUPPORTED_CCYPS = Map.of(
		CurrencyPair.BTC_JPY, "BTC_JPY",
		CurrencyPair.ETH_BTC, "ETH_BTC",
		CurrencyPair.BCH_BTC, "BCH_BTC");

	protected String placeLimitOrder(LimitOrder order) throws Exception {
		String type = BitflyerOrderType.fromOrderType(order.getType()).toString();
		String product = toPairString(order.getCurrencyPair());
		String path = "/v1/me/sendchildorder";
		BitflyerNewOrderRequest request = new BitflyerNewOrderRequest(
			"LIMIT", product, type, order.getOriginalAmount(), order.getLimitPrice());
		String requestBody = OBJECT_MAPPER.writeValueAsString(request);
		try {
			OrderResult orderResult = doPostWithAuth(path, requestBody, OrderResult.class);
			if (orderResult==null) return null;
        	return orderResult.getChildOrderAcceptanceId();
        } catch (Exception e) {
            throw e;
        }
	}

```
### /Users/kh/git/ccy/cryptobot/src/main/java/cryptobot/exchange/bitflyer/Bitflyer.java

```java
import com.fasterxml.jackson.databind.ObjectMapper;
import cryptobot.dto.HealthStatus;
import cryptobot.dto.OrderRecord;
import cryptobot.exchange.OrderbookService;
import cryptobot.misc.JsonUtils;
import cryptobot.misc.MailSender;
import cryptobot.misc.Util;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order.OrderType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

	@Value("${exchange.api.bitflyer:https://api.bitflyer.jp}")
	private String URL_BASE;
	private static final String APPLICATION_JSON = "application/json";
	private static final String ACCESS_KEY = "ACCESS-KEY";
	private static final String ACCESS_TIMESTAMP = "ACCESS-TIMESTAMP";
	private static final String ACCESS_SIGN = "ACCESS-SIGN";
	private static final String CONTENT_TYPE = "Content-Type";
	@Value("${bitflyer.api.key:#{null}}") String API_KEY;
	@Value("${bitflyer.api.secret:#{null}}") String SECRET;
	@Value("${bitflyer.fee.withdraw.counter:#{null}}") BigDecimal COUNTER_WITHDRAW_FEE;
	@Value("${bitflyer.fee.withdraw.base:#{null}}") BigDecimal BASE_WITHDRAW_FEE;
	@Value("${bitflyer.rebalance.threshold.base:0}") BigDecimal rebalanceThresholdBase;
	@Value("${bitflyer.rebalance.target.base:0}") BigDecimal rebalanceTargetBase;
	@Value("${bitflyer.rebalance.threshold.counter:0}") BigDecimal rebalanceThresholdCounter;
	@Value("${bitflyer.rebalance.target.counter:0}") BigDecimal rebalanceTargetCounter;
	@Value("${cryptobot.healthcheck}") private String enableHealthCheck;
	private HealthStatus status;
	@Autowired MailSender mail;
	@Autowired BitflyerOrderbookService orderbookService;
	@Autowired BitflyerWeb web;
	ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(
			Thread.ofVirtual().name("bf_healthcheck").factory());
	private static final ObjectMapper OBJECT_MAPPER = JsonUtils.MAPPER;
	public static enum OrderFilterBy
	public enum BitflyerOrderType
		private static final Map<String, BitflyerOrderType> fromString = new HashMap<String, BitflyerOrderType>();
	static Map<CurrencyPair, String> SUPPORTED_CCYPS = Map.of(
		CurrencyPair.BTC_JPY, "BTC_JPY",
		CurrencyPair.ETH_BTC, "ETH_BTC",
		CurrencyPair.BCH_BTC, "BCH_BTC");

	/**
	 * 指値注文発行後の取得待ち(注文発行後は即時に取得できない)
	 * @param acceptanceId CHILD_ORDER_ACCEPTANCE_ID(注文送信直後なのでCHILD_ORDER_IDが未決定の状態を想定)
	 * @return
	 * @throws Exception
	 */
	@Override
	public OrderRecord waitOrderCreate(String acceptanceId) throws Exception {
		var order = orderPool.waitOrder(acceptanceId, null);
		if (order != null) {
			return order;
		} else {
			return getOrderWithRetry(acceptanceId);
		}
	}

```
### /Users/kh/git/ccy/cryptobot/src/main/java/cryptobot/exchange/bitflyer/BitflyerFX.java

```java
import static java.math.BigDecimal.*;
import static java.math.RoundingMode.*;
import static org.knowm.xchange.currency.Currency.*;
import static org.knowm.xchange.dto.Order.OrderType.*;
import java.math.BigDecimal;
import java.util.*;
import org.knowm.xchange.dto.Order.OrderType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import cryptobot.dto.OrderRecord;
import cryptobot.exchange.OrderbookService;

	private static final String HMAC_SHA256 = "HmacSHA256";
	private static final String URL_BASE = "https://api.bitflyer.jp";
	private static final String APPLICATION_JSON = "application/json";
	private static final String ACCESS_KEY = "ACCESS-KEY";
	private static final String ACCESS_TIMESTAMP = "ACCESS-TIMESTAMP";
	private static final String ACCESS_SIGN = "ACCESS-SIGN";
	private static final String CONTENT_TYPE = "Content-Type";
	private static final String ccyps = "FX_BTC_JPY";
	@Value("${bitflyer.api.key:#{null}}") String API_KEY;
	@Value("${bitflyer.api.secret:#{null}}") String SECRET;
	BigDecimal TRADING_CHARGE = ZERO;
	@Autowired BitflyerFxOrderbookService orderbookService;
	@Autowired Bitflyer bitflyer;
		public OrderType type;
		public BigDecimal price;
		public BigDecimal volume;
		public BigDecimal collateral;
		public Date openDate;
		public Integer leverage;
		public BigDecimal pnl;
		public BigDecimal sfd;

	@Override
	public OrderRecord buySpot(BigDecimal volume, BigDecimal limitPrice) throws Exception {
		return null;
	}

```
### /Users/kh/git/ccy/cryptobot/src/main/java/cryptobot/exchange/bitpoint/Bitpoint.java

```java
import static java.math.BigDecimal.*;
import static java.util.stream.Collectors.*;
import static org.apache.commons.lang3.StringUtils.*;
import static org.knowm.xchange.currency.CurrencyPair.*;
import static org.knowm.xchange.dto.Order.OrderType.*;
import java.math.BigDecimal;
import java.util.Map;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.JsonNode;
import cryptobot.dto.OrderRecord;
import cryptobot.exchange.OrderbookService;
import cryptobot.misc.MailSender;

	private static final String URL_BASE = "https://smartapi.bitpoint.co.jp/bpj-smart-api";
	private static final String APPLICATION_JSON = "application/json";
	private static final String ACCESS_KEY = "access-key";
	private static final String ACCESS_NONCE = "access-nonce";
	private static final String ACCESS_SIGN = "access-signature";
	private static final String CONTENT_TYPE = "Content-Type";
	@Value("${bitpoint.api.key:#{null}}") String API_KEY;
	@Value("${bitpoint.api.secret:#{null}}") String SECRET;
	@Value("${bitpoint.pincode:#{null}}") String PIN_CODE;
	@Value("${bitpoint.rebalance.threshold.base:0}") BigDecimal rebalanceThresholdBase;
	@Value("${bitpoint.rebalance.target.base:0}") BigDecimal rebalanceTargetBase;
	@Value("${bitpoint.rebalance.threshold.counter:0}") BigDecimal rebalanceThresholdCounter;
	@Value("${bitpoint.rebalance.target.counter:0}") BigDecimal rebalanceTargetCounter;
	@Autowired MailSender mail;
	@Autowired BitpointOrderbookService orderbookService;
	static Map<CurrencyPair, String> SUPPORTED_CCYP = Map.of(
			CurrencyPair.BTC_JPY, "BTCJPY",
			CurrencyPair.ETH_JPY, "ETHJPY");
	@Autowired BitpointWeb web;

	@Override
	public OrderRecord buySpot(BigDecimal volume, BigDecimal limitPrice) throws Exception {
		var path = "/api/order";
		var params = Map.of(
				"symbol", SUPPORTED_CCYP.get(BTC_JPY),
				"side", "BUY",
				"type", "LIMIT",
				"timeInForce", "GTC",
				"quantity", volume.toString(),
				"price", limitPrice.toString(),
				"pinCode", PIN_CODE);
		JsonNode result = doHttpPost(path, params);
		var order = createLimitOrder(result);
		return new OrderRecord(this, order);
	}

```
### /Users/kh/git/ccy/cryptobot/src/main/java/cryptobot/exchange/bitpoint/Bitpoint.java

```java
import static java.math.BigDecimal.*;
import static java.util.stream.Collectors.*;
import static org.apache.commons.lang3.StringUtils.*;
import static org.knowm.xchange.currency.CurrencyPair.*;
import static org.knowm.xchange.dto.Order.OrderType.*;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.JsonNode;
import cryptobot.exchange.OrderbookService;
import cryptobot.misc.MailSender;
import cryptobot.misc.JsonUtils;

	private static final String URL_BASE = "https://smartapi.bitpoint.co.jp/bpj-smart-api";
	private static final String APPLICATION_JSON = "application/json";
	private static final String ACCESS_KEY = "access-key";
	private static final String ACCESS_NONCE = "access-nonce";
	private static final String ACCESS_SIGN = "access-signature";
	private static final String CONTENT_TYPE = "Content-Type";
	@Value("${bitpoint.api.key:#{null}}") String API_KEY;
	@Value("${bitpoint.api.secret:#{null}}") String SECRET;
	@Value("${bitpoint.pincode:#{null}}") String PIN_CODE;
	@Value("${bitpoint.rebalance.threshold.base:0}") BigDecimal rebalanceThresholdBase;
	@Value("${bitpoint.rebalance.target.base:0}") BigDecimal rebalanceTargetBase;
	@Value("${bitpoint.rebalance.threshold.counter:0}") BigDecimal rebalanceThresholdCounter;
	@Value("${bitpoint.rebalance.target.counter:0}") BigDecimal rebalanceTargetCounter;
	@Autowired MailSender mail;
	@Autowired BitpointOrderbookService orderbookService;
	static Map<CurrencyPair, String> SUPPORTED_CCYP = Map.of(
			CurrencyPair.BTC_JPY, "BTCJPY",
			CurrencyPair.ETH_JPY, "ETHJPY");
	@Autowired BitpointWeb web;

	private JsonNode doHttpPost(String path, Map<String, String> params) throws Exception {
		long nonce = System.currentTimeMillis();
		var data = new HashMap<>(params);
		data.put("timestamp", String.valueOf(nonce));
		data.put("recvWindow", "10000");
		
		var json = JsonUtils.MAPPER.writeValueAsString(data);
		var url = URL_BASE + path;
		var message = API_KEY + "\n" + nonce + "\n" + (json == null ? "" : json);
		var signature = createHMAC(SECRET, message);

		String body = restClient.post()
				.uri(url)
				.header(ACCESS_KEY, API_KEY)
				.header(ACCESS_NONCE, String.valueOf(nonce))
				.header(ACCESS_SIGN, signature)
				.header(CONTENT_TYPE, APPLICATION_JSON)
				.body(json)
				.retrieve()
				.body(String.class);
		return JsonUtils.MAPPER.readTree(body);
	}

```
### /Users/kh/git/ccy/cryptobot/src/main/java/cryptobot/exchange/ExchangeService.java

```java
import static java.math.BigDecimal.*;
import static java.math.RoundingMode.*;
import static java.util.Optional.*;
import static java.util.stream.Collectors.*;
import static org.knowm.xchange.dto.Order.OrderStatus.*;
import static org.knowm.xchange.dto.Order.OrderType.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import cryptobot.dto.*;
import org.springframework.web.client.RestClient;
import org.knowm.xchange.BaseExchange;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.Order.OrderType;
import org.knowm.xchange.dto.account.Balance;
import org.knowm.xchange.dto.account.Fee;
import org.knowm.xchange.dto.marketdata.Ticker;
import org.knowm.xchange.dto.marketdata.Trade;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.knowm.xchange.service.trade.TradeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import cryptobot.AppConfig;
import cryptobot.misc.MailSender;

	@Value("${cryptobot.currencyPair}") protected CurrencyPair ccyp;
	@Value("${cryptobot.price.precision}") protected int scale;
	@Value("${cryptobot.volume.precision}") protected int volScale;
	protected BaseExchange xchange;
	protected final ConcurrentHashMap<Currency, BalanceInfo> balanceInfoMap = new ConcurrentHashMap<>();
	protected final OrderPool orderPool = new OrderPool();
	protected volatile Ticker lastTick;
	protected final Logger log = LoggerFactory.getLogger(this.getClass());
	private boolean enable = true;
	private boolean allowBid = true;
	private boolean allowAsk = true;
	@Autowired ApplicationEventPublisher publisher;
	@Autowired protected AppConfig config;
	@Autowired protected RestClient restClient;
	@Autowired MailSender mailer;
	protected Fee fee = new Fee(ZERO, ZERO);
	public static enum FeeType
	@Autowired TickerLogger tickerLogger;
		final Set<String> expectedOrderId = new HashSet<>();

	public OrderRecord sellSpot(BigDecimal volume, BigDecimal limitPrice) throws Exception {
		LimitOrder order = new LimitOrder(OrderType.ASK, volume, ccyp, null, null, limitPrice);
		String id = xchange.getTradeService().placeLimitOrder(order);
		return getOrder(id);
	}

```
### /Users/kh/git/ccy/cryptobot/src/main/java/cryptobot/exchange/binance/Binance.java

```java
import cryptobot.dto.OrderRecord;
import cryptobot.exchange.OrderbookService;
import org.knowm.xchange.binance.service.BinanceAccountService;
import org.knowm.xchange.binance.service.BinanceTradeService;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.Order.OrderType;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.knowm.xchange.dto.trade.LimitOrder.Builder;
import org.knowm.xchange.service.trade.TradeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.*;
import static java.math.RoundingMode.HALF_UP;
import static org.knowm.xchange.dto.Order.OrderType.ASK;

	@Autowired BinanceOrderbookService orderbookService;
	@Value("${binance.api.key}") private String API_KEY;
	@Value("${binance.api.secret}") private String SECRET;
	@Value("${binance.fee.withdraw.counter}") BigDecimal COUNTER_WITHDRAW_FEE;
	@Value("${binance.fee.withdraw.base}") BigDecimal BASE_WITHDRAW_FEE;
	@Value("${binance.address.eth:#{null}}") String ETH_ADDRESS;
	@Value("${binance.address.btc:#{null}}") String BTC_ADDRESS;
	@Value("${binance.address.bch:#{null}}") String BCH_ADDRESS;
	@Value("${binance.address.xlm:#{null}}") String XLM_ADDRESS;
	@Value("${binance.address.xlm.memo:#{null}}") String XLM_MEMO;
	@Value("${binance.rebalance.threshold.base:0}") BigDecimal rebalanceThresholdBase;
	@Value("${binance.rebalance.target.base:0}") BigDecimal rebalanceTargetBase;
	@Value("${binance.rebalance.threshold.counter:0}") BigDecimal rebalanceThresholdCounter;
	@Value("${binance.rebalance.target.counter:0}") BigDecimal rebalanceTargetCounter;
	BinanceTradeService tradeService;
	BinanceAccountService accountService;
	private final int stepSize = 4;
	@Value("${exchange.api.binance:#{null}}") private String API_ENDPOINT;

	@Override
	synchronized public OrderRecord sellSpot(BigDecimal volume, BigDecimal limitPrice) throws Exception {
		LimitOrder limitOrder = new Builder(OrderType.ASK, ccyp)
				.limitPrice(limitPrice).originalAmount(volume.setScale(stepSize, HALF_UP)).build();
		String orderId = tradeService.placeLimitOrder(limitOrder);
		log.info("order sent. id: {}", orderId);
		return waitOrderCreate(orderId);
	}

```
### /Users/kh/git/ccy/cryptobot/src/main/java/cryptobot/exchange/bitbank/Bitbank.java

```java
import static cryptobot.exchange.ExchangeService.FeeType.*;
import static java.lang.String.*;
import static java.math.BigDecimal.*;
import static java.math.RoundingMode.*;
import static java.util.Objects.*;
import static java.util.stream.Collectors.*;
import static org.apache.commons.lang3.StringUtils.*;
import static org.knowm.xchange.dto.Order.OrderStatus.*;
import static org.knowm.xchange.dto.Order.OrderType.*;
import java.math.BigDecimal;
import java.util.Map;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import cryptobot.dto.OrderRecord;
import cryptobot.exchange.OrderbookService;
import cryptobot.exchange.bitbank.dto.*;
import cryptobot.misc.MailSender;

	@Value("${exchange.api.bitbank:https://api.bitbank.cc}")
	private String API_ENDPOINT;
	@Value("${bitbank.web.2fa.secret}") String SECRET_KEY_FOR_2FA;
	@Value("${bitbank.api.key}") String API_KEY;
	@Value("${bitbank.api.secret}") String SECRET;
	@Value("${bitbank.fee.withdraw.counter}") BigDecimal COUNTER_WITHDRAW_FEE;
	@Value("${bitbank.fee.withdraw.base}") BigDecimal BASE_WITHDRAW_FEE;
	@Value("${bitbank.address.eth:#{null}}") String ETH_ADDRESS;
	@Value("${bitbank.address.bch:#{null}}") String BCH_ADDRESS;
	@Value("${bitbank.address.btc:#{null}}") String BTC_ADDRESS;
	@Value("${bitbank.rebalance.threshold.base:0}") BigDecimal rebalanceThresholdBase;
	@Value("${bitbank.rebalance.target.base:0}") BigDecimal rebalanceTargetBase;
	@Value("${bitbank.rebalance.threshold.counter:0}") BigDecimal rebalanceThresholdCounter;
	@Value("${bitbank.rebalance.target.counter:0}") BigDecimal rebalanceTargetCounter;
	@Value("${cryptobot.healthcheck}") private String enableHealthCheck;
	@Autowired MailSender mail;
	@Autowired BitbankOrderbookService orderbookService;
	BitbankAssets assetsResponse = null;
	String ccyps;
	private Map<String, String> errorCodes;
	static Map<CurrencyPair, String> SUPPORTED_CCYPS = Map.of(
			CurrencyPair.BTC_JPY, "BTC_JPY"
	);

	@Override
	synchronized public OrderRecord sellSpot(BigDecimal volume, BigDecimal limitPrice) throws Exception {
		return executeOrder(volume, limitPrice, BitbankOrders.Side.SELL, BitbankOrders.Type.LIMIT);
	}

```
### /Users/kh/git/ccy/cryptobot/src/main/java/cryptobot/exchange/ExchangeService.java

```java
import static java.math.BigDecimal.*;
import static java.math.RoundingMode.*;
import static java.util.Optional.*;
import static java.util.stream.Collectors.*;
import static org.knowm.xchange.dto.Order.OrderStatus.*;
import static org.knowm.xchange.dto.Order.OrderType.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import cryptobot.dto.*;
import org.springframework.web.client.RestClient;
import org.knowm.xchange.BaseExchange;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.Order.OrderType;
import org.knowm.xchange.dto.account.Balance;
import org.knowm.xchange.dto.account.Fee;
import org.knowm.xchange.dto.marketdata.Ticker;
import org.knowm.xchange.dto.marketdata.Trade;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.knowm.xchange.service.trade.TradeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import cryptobot.AppConfig;
import cryptobot.misc.MailSender;

	@Value("${cryptobot.currencyPair}") protected CurrencyPair ccyp;
	@Value("${cryptobot.price.precision}") protected int scale;
	@Value("${cryptobot.volume.precision}") protected int volScale;
	protected BaseExchange xchange;
	protected final ConcurrentHashMap<Currency, BalanceInfo> balanceInfoMap = new ConcurrentHashMap<>();
	protected final OrderPool orderPool = new OrderPool();
	protected volatile Ticker lastTick;
	protected final Logger log = LoggerFactory.getLogger(this.getClass());
	private boolean enable = true;
	private boolean allowBid = true;
	private boolean allowAsk = true;
	@Autowired ApplicationEventPublisher publisher;
	@Autowired protected AppConfig config;
	@Autowired protected RestClient restClient;
	@Autowired MailSender mailer;
	protected Fee fee = new Fee(ZERO, ZERO);
	public static enum FeeType
	@Autowired TickerLogger tickerLogger;
		final Set<String> expectedOrderId = new HashSet<>();

	public OrderRecord sellSpot(BigDecimal volume, BigDecimal limitPrice) throws Exception {
		LimitOrder order = new LimitOrder(OrderType.ASK, volume, ccyp, null, null, limitPrice);
		String id = xchange.getTradeService().placeLimitOrder(order);
		return getOrder(id);
	}

```
### /Users/kh/git/ccy/cryptobot/src/main/java/cryptobot/exchange/bitfinex/Bitfinex.java

```java
import static cryptobot.exchange.ExchangeService.FeeType.*;
import static java.math.BigDecimal.*;
import static java.math.RoundingMode.*;
import static java.util.Objects.*;
import static java.util.stream.Collectors.*;
import static org.knowm.xchange.dto.Order.OrderStatus.*;
import static org.knowm.xchange.dto.Order.OrderType.*;
import java.math.BigDecimal;
import java.util.*;
import org.knowm.xchange.bitfinex.service.BitfinexTradeService;
import org.knowm.xchange.bitfinex.v1.BitfinexOrderType;
import org.knowm.xchange.bitfinex.v1.dto.trade.BitfinexOrderStatusResponse;
import org.knowm.xchange.dto.Order.OrderStatus;
import org.knowm.xchange.dto.Order.OrderType;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import cryptobot.dto.OrderRecord;
import cryptobot.exchange.OrderbookService;

	private static final BigDecimal TRADING_CHARGE = new BigDecimal("0.00200000");
	private BitfinexTradeService tradeService;
	@Value("${bitfinex.api.key}") private String API_KEY;
	@Value("${bitfinex.api.secret}") private String SECRET;
	@Value("${bitfinex.fee.withdraw.counter}") BigDecimal COUNTER_WITHDRAW_FEE;
	@Value("${bitfinex.fee.withdraw.base}") BigDecimal BASE_WITHDRAW_FEE;
	@Value("${bitfinex.address.bch:#{null}}") String BCH_ADDRESS;
	@Value("${bitfinex.address.xlm:#{null}}") String XLM_ADDRESS;
	@Value("${bitfinex.address.xlm.memo:#{null}}") String XLM_MEMO;
	@Value("${bitfinex.rebalance.threshold.base:0}") BigDecimal rebalanceThresholdBase;
	@Value("${bitfinex.rebalance.target.base:0}") BigDecimal rebalanceTargetBase;
	@Value("${bitfinex.rebalance.threshold.counter:0}") BigDecimal rebalanceThresholdCounter;
	@Value("${bitfinex.rebalance.target.counter:0}") BigDecimal rebalanceTargetCounter;
	@Autowired BitfinexOrderbookService orderbookService;

	@Override
	public OrderRecord sellSpot(BigDecimal volume, BigDecimal limitPrice) throws Exception {
		LimitOrder limitOrder = new LimitOrder.Builder(OrderType.ASK, ccyp)
				.limitPrice(limitPrice).originalAmount(volume).build();
		BitfinexOrderStatusResponse newOrder = tradeService.placeBitfinexLimitOrder(
				limitOrder, BitfinexOrderType.LIMIT);
		log.info("order executed. id: {}", newOrder);
		return waitOrderCreate(String.valueOf(newOrder.getId()));
	}

```
### /Users/kh/git/ccy/cryptobot/src/main/java/cryptobot/exchange/bitflyer/Bitflyer.java

```java
import com.fasterxml.jackson.databind.ObjectMapper;
import cryptobot.dto.HealthStatus;
import cryptobot.dto.OrderRecord;
import cryptobot.exchange.OrderbookService;
import cryptobot.misc.JsonUtils;
import cryptobot.misc.MailSender;
import cryptobot.misc.Util;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order.OrderType;
import org.knowm.xchange.dto.account.Fee;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import static java.math.BigDecimal.ONE;
import static java.math.RoundingMode.HALF_UP;
import static org.knowm.xchange.dto.Order.OrderType.ASK;

	@Value("${exchange.api.bitflyer:https://api.bitflyer.jp}")
	private String URL_BASE;
	private static final String APPLICATION_JSON = "application/json";
	private static final String ACCESS_KEY = "ACCESS-KEY";
	private static final String ACCESS_TIMESTAMP = "ACCESS-TIMESTAMP";
	private static final String ACCESS_SIGN = "ACCESS-SIGN";
	private static final String CONTENT_TYPE = "Content-Type";
	@Value("${bitflyer.api.key:#{null}}") String API_KEY;
	@Value("${bitflyer.api.secret:#{null}}") String SECRET;
	@Value("${bitflyer.fee.withdraw.counter:#{null}}") BigDecimal COUNTER_WITHDRAW_FEE;
	@Value("${bitflyer.fee.withdraw.base:#{null}}") BigDecimal BASE_WITHDRAW_FEE;
	@Value("${bitflyer.rebalance.threshold.base:0}") BigDecimal rebalanceThresholdBase;
	@Value("${bitflyer.rebalance.target.base:0}") BigDecimal rebalanceTargetBase;
	@Value("${bitflyer.rebalance.threshold.counter:0}") BigDecimal rebalanceThresholdCounter;
	@Value("${bitflyer.rebalance.target.counter:0}") BigDecimal rebalanceTargetCounter;
	@Value("${cryptobot.healthcheck}") private String enableHealthCheck;
	private HealthStatus status;
	@Autowired MailSender mail;
	@Autowired BitflyerOrderbookService orderbookService;
	@Autowired BitflyerWeb web;
	ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(
			Thread.ofVirtual().name("bf_healthcheck").factory());
	private static final ObjectMapper OBJECT_MAPPER = JsonUtils.MAPPER;
	public static enum OrderFilterBy
	public enum BitflyerOrderType
		private static final Map<String, BitflyerOrderType> fromString = new HashMap<String, BitflyerOrderType>();
	static Map<CurrencyPair, String> SUPPORTED_CCYPS = Map.of(
		CurrencyPair.BTC_JPY, "BTC_JPY",
		CurrencyPair.ETH_BTC, "ETH_BTC",
		CurrencyPair.BCH_BTC, "BCH_BTC");

	@Override
	public OrderRecord sellSpot(BigDecimal volume, BigDecimal limitPrice) throws Exception {
		// 取引通貨(base currency)で手数料を徴収されるため、引数で指定された額＝残高減少額になるよう調整
		volume = volume.divide(ONE.add(getFeeRate(FeeType.MAKER)), volScale, HALF_UP);
		LimitOrder order = new LimitOrder.Builder(OrderType.ASK, ccyp)
				.limitPrice(limitPrice).originalAmount(volume).build();
		String acceptanceId = placeLimitOrder(order);
		log.info("order sent. id: {}", acceptanceId);
		return waitOrderCreate(acceptanceId);
	}

```
### /Users/kh/git/ccy/cryptobot/src/main/java/cryptobot/exchange/bitflyer/BitflyerFX.java

```java
import static java.math.BigDecimal.*;
import static java.math.RoundingMode.*;
import static org.knowm.xchange.currency.Currency.*;
import static org.knowm.xchange.dto.Order.OrderType.*;
import java.math.BigDecimal;
import java.util.*;
import org.knowm.xchange.dto.Order.OrderType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import cryptobot.dto.OrderRecord;
import cryptobot.exchange.OrderbookService;

	private static final String HMAC_SHA256 = "HmacSHA256";
	private static final String URL_BASE = "https://api.bitflyer.jp";
	private static final String APPLICATION_JSON = "application/json";
	private static final String ACCESS_KEY = "ACCESS-KEY";
	private static final String ACCESS_TIMESTAMP = "ACCESS-TIMESTAMP";
	private static final String ACCESS_SIGN = "ACCESS-SIGN";
	private static final String CONTENT_TYPE = "Content-Type";
	private static final String ccyps = "FX_BTC_JPY";
	@Value("${bitflyer.api.key:#{null}}") String API_KEY;
	@Value("${bitflyer.api.secret:#{null}}") String SECRET;
	BigDecimal TRADING_CHARGE = ZERO;
	@Autowired BitflyerFxOrderbookService orderbookService;
	@Autowired Bitflyer bitflyer;
		public OrderType type;
		public BigDecimal price;
		public BigDecimal volume;
		public BigDecimal collateral;
		public Date openDate;
		public Integer leverage;
		public BigDecimal pnl;
		public BigDecimal sfd;

	@Override
	public OrderRecord sellSpot(BigDecimal volume, BigDecimal limitPrice) throws Exception {
		return null;
	}

```
### /Users/kh/git/ccy/cryptobot/src/main/java/cryptobot/exchange/bitpoint/Bitpoint.java

```java
import static java.math.BigDecimal.*;
import static java.util.stream.Collectors.*;
import static org.apache.commons.lang3.StringUtils.*;
import static org.knowm.xchange.currency.CurrencyPair.*;
import static org.knowm.xchange.dto.Order.OrderType.*;
import java.math.BigDecimal;
import java.util.Map;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.JsonNode;
import cryptobot.dto.OrderRecord;
import cryptobot.exchange.OrderbookService;
import cryptobot.misc.MailSender;

	private static final String URL_BASE = "https://smartapi.bitpoint.co.jp/bpj-smart-api";
	private static final String APPLICATION_JSON = "application/json";
	private static final String ACCESS_KEY = "access-key";
	private static final String ACCESS_NONCE = "access-nonce";
	private static final String ACCESS_SIGN = "access-signature";
	private static final String CONTENT_TYPE = "Content-Type";
	@Value("${bitpoint.api.key:#{null}}") String API_KEY;
	@Value("${bitpoint.api.secret:#{null}}") String SECRET;
	@Value("${bitpoint.pincode:#{null}}") String PIN_CODE;
	@Value("${bitpoint.rebalance.threshold.base:0}") BigDecimal rebalanceThresholdBase;
	@Value("${bitpoint.rebalance.target.base:0}") BigDecimal rebalanceTargetBase;
	@Value("${bitpoint.rebalance.threshold.counter:0}") BigDecimal rebalanceThresholdCounter;
	@Value("${bitpoint.rebalance.target.counter:0}") BigDecimal rebalanceTargetCounter;
	@Autowired MailSender mail;
	@Autowired BitpointOrderbookService orderbookService;
	static Map<CurrencyPair, String> SUPPORTED_CCYP = Map.of(
			CurrencyPair.BTC_JPY, "BTCJPY",
			CurrencyPair.ETH_JPY, "ETHJPY");
	@Autowired BitpointWeb web;

	@Override
	public OrderRecord sellSpot(BigDecimal volume, BigDecimal limitPrice) throws Exception {
		var path = "/api/order";
		var params = Map.of(
				"symbol", SUPPORTED_CCYP.get(BTC_JPY),
				"side", "SELL",
				"type", "LIMIT",
				"timeInForce", "GTC",
				"quantity", volume.toString(),
				"price", limitPrice.toString(),
				"pinCode", PIN_CODE);
		JsonNode result = doHttpPost(path, params);
		var order = createLimitOrder(result);
		return new OrderRecord(this, order);
	}

```
### /Users/kh/git/ccy/cryptobot/src/main/java/cryptobot/misc/MailSender.java

```java
import jakarta.mail.*;
import org.springframework.beans.factory.annotation.Value;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

    @Value("${mail.smtp.userid}") String userid;
    @Value("${mail.smtp.password}") String password;
    @Value("${mail.smtp.host}") String hostname;
    @Value("${mail.smtp.port:587}") String port;
    @Value("${mail.destination}") String destination;
    ExecutorService executor = Executors.newThreadPerTaskExecutor(
            Thread.ofVirtual().name("mail-send").factory());

    public void info(String subject, String body) {
        executor.submit(()->{
            send(subject, body);
        });
    }

```
### /Users/kh/git/ccy/cryptobot/src/main/java/cryptobot/misc/MailSender.java

```java
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import java.io.UnsupportedEncodingException;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

    @Value("${mail.smtp.userid}") String userid;
    @Value("${mail.smtp.password}") String password;
    @Value("${mail.smtp.host}") String hostname;
    @Value("${mail.smtp.port:587}") String port;
    @Value("${mail.destination}") String destination;
    ExecutorService executor = Executors.newThreadPerTaskExecutor(
            Thread.ofVirtual().name("mail-send").factory());

    private void send(String subject, String body) {
        // SMTPサーバーの設定
        Properties props = new Properties();
        props.put("mail.smtp.host", hostname); // Gmail SMTPサーバー
        props.put("mail.smtp.port", port); // TLSポート
        props.put("mail.smtp.auth", "true"); // 認証を有効にする
        props.put("mail.smtp.starttls.enable", "true"); // STARTTLSを有効にする (TLS暗号化)

        // 必要に応じてタイムアウト設定などを追加
        // props.put("mail.smtp.connectiontimeout", "5000");
        // props.put("mail.smtp.timeout", "5000");

        // セッションの取得 (認証情報を含む)
        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(userid, password);
            }
        });

        try {
            // メッセージの作成
            Message message = new MimeMessage(session);
            // 送信元設定 (オプションで表示名も設定可能)
            try {
                message.setFrom(new InternetAddress(userid, "Cryptobot")); // 必要に応じて表示名を変更
            } catch (UnsupportedEncodingException e) {
                message.setFrom(new InternetAddress(userid));
            }
            // 送信先設定
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destination));
            // 件名設定
            message.setSubject(subject);
            // 本文設定
            message.setText(body);
            // メールの送信
            Transport.send(message);

        } catch (MessagingException e) {
            log.error("メール送信中にエラーが発生しました。", e);
            // 考えられる原因:
            // - 認証情報 (ユーザー名、アプリパスワード) が間違っている
            // - ネットワーク接続の問題
            // - Gmail側の設定 (2段階認証、アプリパスワードが正しく設定されていない)
            // - ファイアウォールによるブロック
            // - ライブラリの不足
        }
    }

```
### /Users/kh/git/ccy/cryptobot/src/main/java/cryptobot/exchange/ExchangeService.java

```java
import static java.math.BigDecimal.*;
import static java.math.RoundingMode.*;
import static java.util.Optional.*;
import static java.util.stream.Collectors.*;
import static org.knowm.xchange.dto.Order.OrderStatus.*;
import static org.knowm.xchange.dto.Order.OrderType.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import cryptobot.dto.*;
import org.springframework.web.client.RestClient;
import org.knowm.xchange.BaseExchange;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.account.Balance;
import org.knowm.xchange.dto.account.Fee;
import org.knowm.xchange.dto.marketdata.Ticker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import cryptobot.AppConfig;
import cryptobot.misc.MailSender;

	@Value("${cryptobot.currencyPair}") protected CurrencyPair ccyp;
	@Value("${cryptobot.price.precision}") protected int scale;
	@Value("${cryptobot.volume.precision}") protected int volScale;
	protected BaseExchange xchange;
	protected final ConcurrentHashMap<Currency, BalanceInfo> balanceInfoMap = new ConcurrentHashMap<>();
	protected final OrderPool orderPool = new OrderPool();
	protected volatile Ticker lastTick;
	protected final Logger log = LoggerFactory.getLogger(this.getClass());
	private boolean enable = true;
	private boolean allowBid = true;
	private boolean allowAsk = true;
	@Autowired ApplicationEventPublisher publisher;
	@Autowired protected AppConfig config;
	@Autowired protected RestClient restClient;
	@Autowired MailSender mailer;
	protected Fee fee = new Fee(ZERO, ZERO);
	public static enum FeeType
	@Autowired TickerLogger tickerLogger;
		final Set<String> expectedOrderId = new HashSet<>();

	public String getName() {
		return this.getClass().getSimpleName();
	}

```
### /Users/kh/git/ccy/cryptobot/src/main/java/cryptobot/exchange/ExchangeService.java

```java
import static java.math.BigDecimal.*;
import static java.math.RoundingMode.*;
import static java.util.Optional.*;
import static java.util.stream.Collectors.*;
import static org.knowm.xchange.dto.Order.OrderStatus.*;
import static org.knowm.xchange.dto.Order.OrderType.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import cryptobot.dto.*;
import org.springframework.web.client.RestClient;
import org.knowm.xchange.BaseExchange;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.account.Balance;
import org.knowm.xchange.dto.account.Fee;
import org.knowm.xchange.dto.marketdata.Ticker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import cryptobot.AppConfig;
import cryptobot.misc.MailSender;

	@Value("${cryptobot.currencyPair}") protected CurrencyPair ccyp;
	@Value("${cryptobot.price.precision}") protected int scale;
	@Value("${cryptobot.volume.precision}") protected int volScale;
	protected BaseExchange xchange;
	protected final ConcurrentHashMap<Currency, BalanceInfo> balanceInfoMap = new ConcurrentHashMap<>();
	protected final OrderPool orderPool = new OrderPool();
	protected volatile Ticker lastTick;
	protected final Logger log = LoggerFactory.getLogger(this.getClass());
	private boolean enable = true;
	private boolean allowBid = true;
	private boolean allowAsk = true;
	@Autowired ApplicationEventPublisher publisher;
	@Autowired protected AppConfig config;
	@Autowired protected RestClient restClient;
	@Autowired MailSender mailer;
	protected Fee fee = new Fee(ZERO, ZERO);
	public static enum FeeType
	@Autowired TickerLogger tickerLogger;
		final Set<String> expectedOrderId = new HashSet<>();

	public String getName() {
		return this.getClass().getSimpleName();
	}

```
### /Users/kh/git/ccy/cryptobot/src/main/java/cryptobot/exchange/ExchangeService.java

```java
import static java.math.BigDecimal.*;
import static java.math.RoundingMode.*;
import static java.util.Optional.*;
import static java.util.stream.Collectors.*;
import static org.knowm.xchange.dto.Order.OrderStatus.*;
import static org.knowm.xchange.dto.Order.OrderType.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import cryptobot.dto.*;
import org.springframework.web.client.RestClient;
import org.knowm.xchange.BaseExchange;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.account.Balance;
import org.knowm.xchange.dto.account.Fee;
import org.knowm.xchange.dto.marketdata.Ticker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import cryptobot.AppConfig;
import cryptobot.misc.MailSender;

	@Value("${cryptobot.currencyPair}") protected CurrencyPair ccyp;
	@Value("${cryptobot.price.precision}") protected int scale;
	@Value("${cryptobot.volume.precision}") protected int volScale;
	protected BaseExchange xchange;
	protected final ConcurrentHashMap<Currency, BalanceInfo> balanceInfoMap = new ConcurrentHashMap<>();
	protected final OrderPool orderPool = new OrderPool();
	protected volatile Ticker lastTick;
	protected final Logger log = LoggerFactory.getLogger(this.getClass());
	private boolean enable = true;
	private boolean allowBid = true;
	private boolean allowAsk = true;
	@Autowired ApplicationEventPublisher publisher;
	@Autowired protected AppConfig config;
	@Autowired protected RestClient restClient;
	@Autowired MailSender mailer;
	protected Fee fee = new Fee(ZERO, ZERO);
	public static enum FeeType
	@Autowired TickerLogger tickerLogger;
		final Set<String> expectedOrderId = new HashSet<>();

	abstract protected OrderbookService getOrderbookService();

```
### /Users/kh/git/ccy/cryptobot/src/main/java/cryptobot/exchange/binance/Binance.java

```java
import cryptobot.exchange.OrderbookService;
import org.knowm.xchange.binance.service.BinanceAccountService;
import org.knowm.xchange.binance.service.BinanceTradeService;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.service.trade.TradeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.*;

	@Autowired BinanceOrderbookService orderbookService;
	@Value("${binance.api.key}") private String API_KEY;
	@Value("${binance.api.secret}") private String SECRET;
	@Value("${binance.fee.withdraw.counter}") BigDecimal COUNTER_WITHDRAW_FEE;
	@Value("${binance.fee.withdraw.base}") BigDecimal BASE_WITHDRAW_FEE;
	@Value("${binance.address.eth:#{null}}") String ETH_ADDRESS;
	@Value("${binance.address.btc:#{null}}") String BTC_ADDRESS;
	@Value("${binance.address.bch:#{null}}") String BCH_ADDRESS;
	@Value("${binance.address.xlm:#{null}}") String XLM_ADDRESS;
	@Value("${binance.address.xlm.memo:#{null}}") String XLM_MEMO;
	@Value("${binance.rebalance.threshold.base:0}") BigDecimal rebalanceThresholdBase;
	@Value("${binance.rebalance.target.base:0}") BigDecimal rebalanceTargetBase;
	@Value("${binance.rebalance.threshold.counter:0}") BigDecimal rebalanceThresholdCounter;
	@Value("${binance.rebalance.target.counter:0}") BigDecimal rebalanceTargetCounter;
	BinanceTradeService tradeService;
	BinanceAccountService accountService;
	private final int stepSize = 4;
	@Value("${exchange.api.binance:#{null}}") private String API_ENDPOINT;

	@Override
	protected OrderbookService getOrderbookService() {
		return orderbookService;
	}

```
### /Users/kh/git/ccy/cryptobot/src/main/java/cryptobot/exchange/bitbank/Bitbank.java

```java
import static cryptobot.exchange.ExchangeService.FeeType.*;
import static java.lang.String.*;
import static java.math.BigDecimal.*;
import static java.math.RoundingMode.*;
import static java.util.Objects.*;
import static java.util.stream.Collectors.*;
import static org.apache.commons.lang3.StringUtils.*;
import static org.knowm.xchange.dto.Order.OrderStatus.*;
import static org.knowm.xchange.dto.Order.OrderType.*;
import java.math.BigDecimal;
import java.util.Map;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import cryptobot.exchange.OrderbookService;
import cryptobot.exchange.bitbank.dto.*;
import cryptobot.misc.MailSender;

	@Value("${exchange.api.bitbank:https://api.bitbank.cc}")
	private String API_ENDPOINT;
	@Value("${bitbank.web.2fa.secret}") String SECRET_KEY_FOR_2FA;
	@Value("${bitbank.api.key}") String API_KEY;
	@Value("${bitbank.api.secret}") String SECRET;
	@Value("${bitbank.fee.withdraw.counter}") BigDecimal COUNTER_WITHDRAW_FEE;
	@Value("${bitbank.fee.withdraw.base}") BigDecimal BASE_WITHDRAW_FEE;
	@Value("${bitbank.address.eth:#{null}}") String ETH_ADDRESS;
	@Value("${bitbank.address.bch:#{null}}") String BCH_ADDRESS;
	@Value("${bitbank.address.btc:#{null}}") String BTC_ADDRESS;
	@Value("${bitbank.rebalance.threshold.base:0}") BigDecimal rebalanceThresholdBase;
	@Value("${bitbank.rebalance.target.base:0}") BigDecimal rebalanceTargetBase;
	@Value("${bitbank.rebalance.threshold.counter:0}") BigDecimal rebalanceThresholdCounter;
	@Value("${bitbank.rebalance.target.counter:0}") BigDecimal rebalanceTargetCounter;
	@Value("${cryptobot.healthcheck}") private String enableHealthCheck;
	@Autowired MailSender mail;
	@Autowired BitbankOrderbookService orderbookService;
	BitbankAssets assetsResponse = null;
	String ccyps;
	private Map<String, String> errorCodes;
	static Map<CurrencyPair, String> SUPPORTED_CCYPS = Map.of(
			CurrencyPair.BTC_JPY, "BTC_JPY"
	);

	@Override
	protected OrderbookService getOrderbookService() { return orderbookService; }

```
### /Users/kh/git/ccy/cryptobot/src/main/java/cryptobot/exchange/coincheck/Coincheck.java

```java
import com.fasterxml.jackson.annotation.JsonProperty;
import cryptobot.exchange.OrderbookService;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.*;
import static org.knowm.xchange.currency.CurrencyPair.BTC_JPY;
import static org.knowm.xchange.dto.Order.OrderStatus.*;

    @Value("${coincheck.api.key}") private String API_KEY;
    @Value("${coincheck.api.secret}") private String SECRET;
    @Autowired CoincheckOrderbookService orderbookService;
    static Map<CurrencyPair, String> SUPPORTED_CCYPS = Map.of(BTC_JPY, "BTC_JPY");
        /** id 注文のID（新規注文でのIDと同一です）*/
        @JsonProperty("id")  String orderId;
        /** rate 注文のレート（ null の場合は成り行き注文です）*/
        @JsonProperty("rate")  BigDecimal limitPrice;
        /** pending_amount 注文の未決済の量 */
        @JsonProperty("pending_amount")  BigDecimal outstandingVolume;
        /** pending_market_buy_amount 注文の未決済の量（現物成行買いの場合のみ） */
        @JsonProperty("pending_market_buy_amount")  BigDecimal outstandingMarketVolume;
        /** order_type 注文のタイプ（"sell" or "buy"）*/
        @JsonProperty("order_type")  String orderType;
        /** stop_loss_rate 逆指値レート */
        @JsonProperty("stop_loss_rate")  BigDecimal stop_loss_rate;
        /** pair 取引ペア */
        @JsonProperty("pair")  CurrencyPair currencyPair;
        /** created_at 注文の作成日時 */
        @JsonProperty("created_at")  String timestamp;

    @Override
    protected OrderbookService getOrderbookService() {
        return orderbookService;
    }

```
### /Users/kh/git/ccy/cryptobot/src/main/java/cryptobot/exchange/bitfinex/Bitfinex.java

```java
import static cryptobot.exchange.ExchangeService.FeeType.*;
import static java.math.BigDecimal.*;
import static java.math.RoundingMode.*;
import static java.util.Objects.*;
import static java.util.stream.Collectors.*;
import static org.knowm.xchange.dto.Order.OrderStatus.*;
import static org.knowm.xchange.dto.Order.OrderType.*;
import java.math.BigDecimal;
import java.util.*;
import org.knowm.xchange.bitfinex.service.BitfinexTradeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import cryptobot.exchange.OrderbookService;

	private static final BigDecimal TRADING_CHARGE = new BigDecimal("0.00200000");
	private BitfinexTradeService tradeService;
	@Value("${bitfinex.api.key}") private String API_KEY;
	@Value("${bitfinex.api.secret}") private String SECRET;
	@Value("${bitfinex.fee.withdraw.counter}") BigDecimal COUNTER_WITHDRAW_FEE;
	@Value("${bitfinex.fee.withdraw.base}") BigDecimal BASE_WITHDRAW_FEE;
	@Value("${bitfinex.address.bch:#{null}}") String BCH_ADDRESS;
	@Value("${bitfinex.address.xlm:#{null}}") String XLM_ADDRESS;
	@Value("${bitfinex.address.xlm.memo:#{null}}") String XLM_MEMO;
	@Value("${bitfinex.rebalance.threshold.base:0}") BigDecimal rebalanceThresholdBase;
	@Value("${bitfinex.rebalance.target.base:0}") BigDecimal rebalanceTargetBase;
	@Value("${bitfinex.rebalance.threshold.counter:0}") BigDecimal rebalanceThresholdCounter;
	@Value("${bitfinex.rebalance.target.counter:0}") BigDecimal rebalanceTargetCounter;
	@Autowired BitfinexOrderbookService orderbookService;

	@Override
	protected OrderbookService getOrderbookService() {
		return orderbookService;
	}

```
### /Users/kh/git/ccy/cryptobot/src/main/java/cryptobot/exchange/bitflyer/Bitflyer.java

```java
import com.fasterxml.jackson.databind.ObjectMapper;
import cryptobot.dto.HealthStatus;
import cryptobot.exchange.OrderbookService;
import cryptobot.misc.JsonUtils;
import cryptobot.misc.MailSender;
import cryptobot.misc.Util;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order.OrderType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

	@Value("${exchange.api.bitflyer:https://api.bitflyer.jp}")
	private String URL_BASE;
	private static final String APPLICATION_JSON = "application/json";
	private static final String ACCESS_KEY = "ACCESS-KEY";
	private static final String ACCESS_TIMESTAMP = "ACCESS-TIMESTAMP";
	private static final String ACCESS_SIGN = "ACCESS-SIGN";
	private static final String CONTENT_TYPE = "Content-Type";
	@Value("${bitflyer.api.key:#{null}}") String API_KEY;
	@Value("${bitflyer.api.secret:#{null}}") String SECRET;
	@Value("${bitflyer.fee.withdraw.counter:#{null}}") BigDecimal COUNTER_WITHDRAW_FEE;
	@Value("${bitflyer.fee.withdraw.base:#{null}}") BigDecimal BASE_WITHDRAW_FEE;
	@Value("${bitflyer.rebalance.threshold.base:0}") BigDecimal rebalanceThresholdBase;
	@Value("${bitflyer.rebalance.target.base:0}") BigDecimal rebalanceTargetBase;
	@Value("${bitflyer.rebalance.threshold.counter:0}") BigDecimal rebalanceThresholdCounter;
	@Value("${bitflyer.rebalance.target.counter:0}") BigDecimal rebalanceTargetCounter;
	@Value("${cryptobot.healthcheck}") private String enableHealthCheck;
	private HealthStatus status;
	@Autowired MailSender mail;
	@Autowired BitflyerOrderbookService orderbookService;
	@Autowired BitflyerWeb web;
	ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(
			Thread.ofVirtual().name("bf_healthcheck").factory());
	private static final ObjectMapper OBJECT_MAPPER = JsonUtils.MAPPER;
	public static enum OrderFilterBy
	public enum BitflyerOrderType
		private static final Map<String, BitflyerOrderType> fromString = new HashMap<String, BitflyerOrderType>();
	static Map<CurrencyPair, String> SUPPORTED_CCYPS = Map.of(
		CurrencyPair.BTC_JPY, "BTC_JPY",
		CurrencyPair.ETH_BTC, "ETH_BTC",
		CurrencyPair.BCH_BTC, "BCH_BTC");

	@Override
	protected OrderbookService getOrderbookService() {
		return orderbookService;
	}

```
### /Users/kh/git/ccy/cryptobot/src/main/java/cryptobot/exchange/bitflyer/BitflyerFX.java

```java
import static java.math.BigDecimal.*;
import static java.math.RoundingMode.*;
import static org.knowm.xchange.currency.Currency.*;
import static org.knowm.xchange.dto.Order.OrderType.*;
import java.math.BigDecimal;
import java.util.*;
import org.knowm.xchange.dto.Order.OrderType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import cryptobot.exchange.OrderbookService;

	private static final String HMAC_SHA256 = "HmacSHA256";
	private static final String URL_BASE = "https://api.bitflyer.jp";
	private static final String APPLICATION_JSON = "application/json";
	private static final String ACCESS_KEY = "ACCESS-KEY";
	private static final String ACCESS_TIMESTAMP = "ACCESS-TIMESTAMP";
	private static final String ACCESS_SIGN = "ACCESS-SIGN";
	private static final String CONTENT_TYPE = "Content-Type";
	private static final String ccyps = "FX_BTC_JPY";
	@Value("${bitflyer.api.key:#{null}}") String API_KEY;
	@Value("${bitflyer.api.secret:#{null}}") String SECRET;
	BigDecimal TRADING_CHARGE = ZERO;
	@Autowired BitflyerFxOrderbookService orderbookService;
	@Autowired Bitflyer bitflyer;
		public OrderType type;
		public BigDecimal price;
		public BigDecimal volume;
		public BigDecimal collateral;
		public Date openDate;
		public Integer leverage;
		public BigDecimal pnl;
		public BigDecimal sfd;

	@Override
	protected OrderbookService getOrderbookService() {
		return orderbookService;
	}

```
### /Users/kh/git/ccy/cryptobot/src/main/java/cryptobot/exchange/bitpoint/Bitpoint.java

```java
import static java.math.BigDecimal.*;
import static java.util.stream.Collectors.*;
import static org.apache.commons.lang3.StringUtils.*;
import static org.knowm.xchange.currency.CurrencyPair.*;
import static org.knowm.xchange.dto.Order.OrderType.*;
import java.math.BigDecimal;
import java.util.Map;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import cryptobot.exchange.OrderbookService;
import cryptobot.misc.MailSender;

	private static final String URL_BASE = "https://smartapi.bitpoint.co.jp/bpj-smart-api";
	private static final String APPLICATION_JSON = "application/json";
	private static final String ACCESS_KEY = "access-key";
	private static final String ACCESS_NONCE = "access-nonce";
	private static final String ACCESS_SIGN = "access-signature";
	private static final String CONTENT_TYPE = "Content-Type";
	@Value("${bitpoint.api.key:#{null}}") String API_KEY;
	@Value("${bitpoint.api.secret:#{null}}") String SECRET;
	@Value("${bitpoint.pincode:#{null}}") String PIN_CODE;
	@Value("${bitpoint.rebalance.threshold.base:0}") BigDecimal rebalanceThresholdBase;
	@Value("${bitpoint.rebalance.target.base:0}") BigDecimal rebalanceTargetBase;
	@Value("${bitpoint.rebalance.threshold.counter:0}") BigDecimal rebalanceThresholdCounter;
	@Value("${bitpoint.rebalance.target.counter:0}") BigDecimal rebalanceTargetCounter;
	@Autowired MailSender mail;
	@Autowired BitpointOrderbookService orderbookService;
	static Map<CurrencyPair, String> SUPPORTED_CCYP = Map.of(
			CurrencyPair.BTC_JPY, "BTCJPY",
			CurrencyPair.ETH_JPY, "ETHJPY");
	@Autowired BitpointWeb web;

	@Override
	protected OrderbookService getOrderbookService() {
		return orderbookService;
	}

```
### /Users/kh/git/ccy/cryptobot/src/main/java/cryptobot/exchange/ExchangeService.java

```java
import static java.math.BigDecimal.*;
import static java.math.RoundingMode.*;
import static java.util.Optional.*;
import static java.util.stream.Collectors.*;
import static org.knowm.xchange.dto.Order.OrderStatus.*;
import static org.knowm.xchange.dto.Order.OrderType.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import cryptobot.dto.*;
import org.springframework.web.client.RestClient;
import org.knowm.xchange.BaseExchange;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.Order.OrderType;
import org.knowm.xchange.dto.account.Balance;
import org.knowm.xchange.dto.account.Fee;
import org.knowm.xchange.dto.marketdata.Ticker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import cryptobot.AppConfig;
import cryptobot.misc.MailSender;

	@Value("${cryptobot.currencyPair}") protected CurrencyPair ccyp;
	@Value("${cryptobot.price.precision}") protected int scale;
	@Value("${cryptobot.volume.precision}") protected int volScale;
	protected BaseExchange xchange;
	protected final ConcurrentHashMap<Currency, BalanceInfo> balanceInfoMap = new ConcurrentHashMap<>();
	protected final OrderPool orderPool = new OrderPool();
	protected volatile Ticker lastTick;
	protected final Logger log = LoggerFactory.getLogger(this.getClass());
	private boolean enable = true;
	private boolean allowBid = true;
	private boolean allowAsk = true;
	@Autowired ApplicationEventPublisher publisher;
	@Autowired protected AppConfig config;
	@Autowired protected RestClient restClient;
	@Autowired MailSender mailer;
	protected Fee fee = new Fee(ZERO, ZERO);
	public static enum FeeType
	@Autowired TickerLogger tickerLogger;
		final Set<String> expectedOrderId = new HashSet<>();

	/**
	 * 成行注文
	 * @param type 注文の方向
	 * @param volume 数量
	 * @return 注文送信後に注文情報を取得した結果の注文レコード
	 * @throws Exception
	 */
	public OrderRecord order(OrderType type, BigDecimal volume) throws Exception {
		var bookType = type.getOpposite();
		getOrderbookService().takeOrder(bookType, volume);
		OrderRecord order;
        try {
            if (type.equals(OrderType.BID)) {
                order = buySpot(volume.setScale(volScale, HALF_UP));
            } else {
                order = sellSpot(volume.setScale(volScale, HALF_UP));
            }
			log.info("market order created. order: {}", order);
			var price = order.getAveragePrice();
            getOrderbookService().correctBestPrice(bookType, price);
            return order;
        } catch (Exception e) {
			mailer.info("exchange disabled on order error", "disabled exchange: " + getName());
			setEnable(false);
			throw e;
        }
    }

```

