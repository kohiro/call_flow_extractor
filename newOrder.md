# Data Flow Extraction

**Start:** `cryptobot.web.view.pages.MarketView#newOrder`

<a id="srcmainjavacryptobotexchangeexchangeservicejava-exchangeservicegetorder"></a>
### src/main/java/cryptobot/exchange/ExchangeService.java (ExchangeService#getOrder)

```java
// --- インポート ---
import static java.math.BigDecimal.*;
import static java.math.RoundingMode.*;
import static java.util.Optional.*;
import static java.util.stream.Collectors.*;
import static org.knowm.xchange.dto.Order.OrderStatus.*;
import static org.knowm.xchange.dto.Order.OrderType.*;
import java.util.*;
import cryptobot.dto.*;
import org.knowm.xchange.dto.Order;

// --- 所属クラス ---
// class ExchangeService extends GenericService

// --- メソッド定義 ---
abstract public OrderRecord getOrder(String id) throws Exception;

```
<a id="srcmainjavacryptobotexchangebinancebinancejava-binancegetorder"></a>
### src/main/java/cryptobot/exchange/binance/Binance.java (Binance#getOrder)

```java
// --- インポート ---
import cryptobot.dto.OrderRecord;
import org.knowm.xchange.binance.service.BinanceTradeService;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.knowm.xchange.dto.trade.LimitOrder.Builder;
import org.knowm.xchange.instrument.Instrument;
import org.knowm.xchange.service.trade.TradeService;
import org.knowm.xchange.service.trade.params.orders.OrderQueryParamInstrument;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.*;

// --- 所属クラス ---
// class Binance extends ExchangeService

// --- フィールド ---
BinanceTradeService tradeService;

// --- メソッド定義 ---
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
<a id="srcmainjavacryptobotexchangebitbankbitbankjava-bitbankmakeprivaterequestheaders"></a>
### src/main/java/cryptobot/exchange/bitbank/Bitbank.java (Bitbank#makePrivateRequestHeaders)

```java
// --- インポート ---
import static cryptobot.exchange.ExchangeService.FeeType.*;
import static java.lang.String.*;
import static java.math.BigDecimal.*;
import static java.math.RoundingMode.*;
import static java.util.Objects.*;
import static java.util.stream.Collectors.*;
import static org.apache.commons.lang3.StringUtils.*;
import static org.knowm.xchange.dto.Order.OrderStatus.*;
import static org.knowm.xchange.dto.Order.OrderType.*;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import cryptobot.exchange.bitbank.dto.*;

// --- 所属クラス ---
// class Bitbank extends ExchangeService

// --- フィールド ---
@Value("${bitbank.api.key}") String API_KEY;

// --- メソッド定義 ---
private Map<String, String> makePrivateRequestHeaders(long nonce, String sign) {
	return Map.of(
			"Content-Type", "application/json; charset=utf-8",
			"ACCESS-KEY", API_KEY,
			"ACCESS-NONCE", String.valueOf(nonce),
			"ACCESS-SIGNATURE", sign);
}

```
<a id="srcmainjavacryptobotexchangegenericservicejava-genericservicecreatehmac"></a>
### src/main/java/cryptobot/exchange/GenericService.java (GenericService#createHMAC)

```java
// --- インポート ---
import static java.math.BigDecimal.*;
import static org.knowm.xchange.dto.Order.OrderStatus.*;
import org.apache.commons.codec.binary.Hex;
import org.jetbrains.annotations.NotNull;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

// --- 所属クラス ---
// class GenericService

// --- メソッド定義 ---
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
<a id="srcmainjavacryptobotexchangebitbankbitbankjava-bitbankgetprivaterequestheader"></a>
### src/main/java/cryptobot/exchange/bitbank/Bitbank.java (Bitbank#getPrivateRequestHeader)

```java
// --- インポート ---
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
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import cryptobot.exchange.bitbank.dto.*;

// --- 所属クラス ---
// class Bitbank extends ExchangeService

// --- フィールド ---
@Value("${bitbank.api.secret}") String SECRET;

// --- メソッド定義 ---
protected Map<String, String> getPrivateRequestHeader(String path, Map<String, String> query) {
	long nonce = currentTimeMillis();
	String queryString = query.entrySet().stream()
			.map(e -> e.getKey() + "=" + e.getValue()).collect(joining("&"));
	if (!queryString.isEmpty()) queryString = "?" + queryString;
	String message = String.valueOf(nonce) + path + queryString;
	return makePrivateRequestHeaders(nonce, createHMAC(SECRET, message)); // -> [Bitbank.java (makePrivateRequestHeaders)](#srcmainjavacryptobotexchangebitbankbitbankjava-bitbankmakeprivaterequestheaders), [GenericService.java (createHMAC)](#srcmainjavacryptobotexchangegenericservicejava-genericservicecreatehmac)
}

```
<a id="srcmainjavacryptobotexchangebitbankbitbankjava-bitbankdohttpget"></a>
### src/main/java/cryptobot/exchange/bitbank/Bitbank.java (Bitbank#doHttpGet)

```java
// --- インポート ---
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
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import com.fasterxml.jackson.databind.JsonNode;
import cryptobot.exchange.bitbank.dto.*;
import cryptobot.misc.JsonUtils;
import org.springframework.web.client.RestClient.ResponseSpec.ErrorHandler;

// --- 所属クラス ---
// class Bitbank extends ExchangeService

// --- フィールド ---
@Value("${exchange.api.bitbank:https://api.bitbank.cc}")
private String API_ENDPOINT;

// --- メソッド定義 ---
protected <T> T doHttpGet(String path, Class<T> clazz, Map<String, String> params) throws Exception {
	Map<String, String> query = params != null ? params : Map.of();
	Map<String, String> headers = getPrivateRequestHeader(path, query); // -> [Bitbank.java (getPrivateRequestHeader)](#srcmainjavacryptobotexchangebitbankbitbankjava-bitbankgetprivaterequestheader)
	
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
<a id="srcmainjavacryptobotexchangebitbankbitbankjava-bitbankgetorder"></a>
### src/main/java/cryptobot/exchange/bitbank/Bitbank.java (Bitbank#getOrder)

```java
// --- インポート ---
import static cryptobot.exchange.ExchangeService.FeeType.*;
import static java.lang.String.*;
import static java.math.BigDecimal.*;
import static java.math.RoundingMode.*;
import static java.util.Objects.*;
import static java.util.stream.Collectors.*;
import static org.apache.commons.lang3.StringUtils.*;
import static org.knowm.xchange.dto.Order.OrderStatus.*;
import static org.knowm.xchange.dto.Order.OrderType.*;
import java.util.Map;
import org.knowm.xchange.dto.trade.LimitOrder;
import cryptobot.dto.OrderRecord;
import cryptobot.exchange.PlacedOrderNotFoundException;
import cryptobot.exchange.bitbank.dto.*;

// --- 所属クラス ---
// class Bitbank extends ExchangeService

// --- フィールド ---
String ccyps;

// --- メソッド定義 ---
@Override
synchronized public OrderRecord getOrder(String id) throws Exception {
	var path = "/v1/user/spot/order";
	var order = doHttpGet(path, BitbankOrders.Order.class, Map.of("pair", ccyps, "order_id", id)); // -> [Bitbank.java (doHttpGet)](#srcmainjavacryptobotexchangebitbankbitbankjava-bitbankdohttpget)
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
<a id="srcmainjavacryptobotexchangecoincheckcoincheckjava-coinchecksendrequest"></a>
### src/main/java/cryptobot/exchange/coincheck/Coincheck.java (Coincheck#sendRequest)

```java
// --- インポート ---
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.*;
import static org.knowm.xchange.dto.Order.OrderStatus.*;

// --- 所属クラス ---
// class Coincheck extends ExchangeService

// --- フィールド ---
@Value("${coincheck.api.key}") private String API_KEY;
@Value("${coincheck.api.secret}") private String SECRET;

// --- メソッド定義 ---
private <T> T sendRequest(String path, Class<T> responseClass) {
    var url = "https://coincheck.com" + path;
    var nonce = String.valueOf(new Date().getTime());
    var signature = createHMAC(SECRET, nonce + url); // -> [GenericService.java (createHMAC)](#srcmainjavacryptobotexchangegenericservicejava-genericservicecreatehmac)

    return restClient.get()
            .uri(url)
            .header("ACCESS-KEY", API_KEY)
            .header("ACCESS-NONCE", nonce)
            .header("ACCESS-SIGNATURE", signature)
            .retrieve()
            .body(responseClass);
}

```
<a id="srcmainjavacryptobotexchangecoincheckcoincheckjava-coincheckparsedatetime"></a>
### src/main/java/cryptobot/exchange/coincheck/Coincheck.java (Coincheck#parseDatetime)

```java
// --- インポート ---
import org.apache.commons.lang3.time.DateFormatUtils;
import java.text.ParseException;
import java.util.*;
import static org.knowm.xchange.dto.Order.OrderStatus.*;

// --- 所属クラス ---
// class Coincheck extends ExchangeService

// --- メソッド定義 ---
private static Date parseDatetime(String datetime) {
    try {
        return DateFormatUtils.ISO_8601_EXTENDED_DATETIME_FORMAT
                .parse(datetime);
    } catch (ParseException e) {
        throw new RuntimeException(e);
    }
}

```
<a id="srcmainjavacryptobotexchangecoincheckcoincheckjava-coincheckgetopenorders"></a>
### src/main/java/cryptobot/exchange/coincheck/Coincheck.java (Coincheck#getOpenOrders)

```java
// --- インポート ---
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order.OrderType;
import org.knowm.xchange.dto.trade.LimitOrder;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import static java.math.BigDecimal.ZERO;
import static org.knowm.xchange.coincheck.CoincheckAdapter.createOrderType;
import static org.knowm.xchange.dto.Order.OrderStatus.*;

// --- 所属クラス ---
// class Coincheck extends ExchangeService

// --- フィールド ---
/** rate 注文のレート（ null の場合は成り行き注文です）*/
@JsonProperty("rate")  BigDecimal limitPrice;
/** order_type 注文のタイプ（"sell" or "buy"）*/
@JsonProperty("order_type")  String orderType;
/** created_at 注文の作成日時 */
@JsonProperty("created_at")  String timestamp;

// --- メソッド定義 ---
@Override
public List<LimitOrder> getOpenOrders() throws IOException {
    var path = "/api/exchange/orders/opens";
    JsonNode json = sendRequest(path, JsonNode.class); // -> [Coincheck.java (sendRequest)](#srcmainjavacryptobotexchangecoincheckcoincheckjava-coinchecksendrequest)
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
            .timestamp(parseDatetime(o.get("created_at").asText())) // -> [Coincheck.java (parseDatetime)](#srcmainjavacryptobotexchangecoincheckcoincheckjava-coincheckparsedatetime)
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
<a id="srcmainjavacryptobotexchangecoincheckcoincheckjava-coincheckgetorder"></a>
### src/main/java/cryptobot/exchange/coincheck/Coincheck.java (Coincheck#getOrder)

```java
// --- インポート ---
import com.fasterxml.jackson.databind.JsonNode;
import cryptobot.dto.OrderRecord;
import org.knowm.xchange.dto.trade.LimitOrder;
import java.util.*;
import static org.knowm.xchange.dto.Order.OrderStatus.*;

// --- 所属クラス ---
// class Coincheck extends ExchangeService

// --- メソッド定義 ---
@Override
public OrderRecord getOrder(String id) throws Exception {
    List<LimitOrder> openOrders =  getOpenOrders(); // -> [Coincheck.java (getOpenOrders)](#srcmainjavacryptobotexchangecoincheckcoincheckjava-coincheckgetopenorders)
    var order = openOrders.stream().filter(o->o.getId().equals(id)).findAny();
    if (order.isPresent()){
        return new OrderRecord(this, order.get());
    }
    var op = orderPool.getById(id);
    if (op != null) {
        var limitOrder = op;
        var result = sendRequest("/api/exchange/orders/cancel_status?id=%s".formatted(id), JsonNode.class); // -> [Coincheck.java (sendRequest)](#srcmainjavacryptobotexchangecoincheckcoincheckjava-coinchecksendrequest)
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
<a id="srcmainjavacryptobotexchangebitfinexbitfinexjava-bitfinexgetopenorders"></a>
### src/main/java/cryptobot/exchange/bitfinex/Bitfinex.java (Bitfinex#getOpenOrders)

```java
// --- インポート ---
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
import org.springframework.stereotype.Service;

// --- 所属クラス ---
// class Bitfinex extends ExchangeService

// --- フィールド ---
private BitfinexTradeService tradeService;

// --- メソッド定義 ---
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
<a id="srcmainjavacryptobotdtoorderrecordjava-orderrecordgetvolume"></a>
### src/main/java/cryptobot/dto/OrderRecord.java (OrderRecord#getVolume)

```java
// --- インポート ---
import java.math.BigDecimal;
import java.util.*;
import static org.knowm.xchange.dto.Order.OrderStatus.*;

// --- 所属クラス ---
// class OrderRecord extends LimitOrder

// --- メソッド定義 ---
public BigDecimal getVolume() {
	return getOriginalAmount();
}

```
<a id="srcmainjavacryptobotdtoorderrecordjava-orderrecordadjuststatusbyexecutionvolume"></a>
### src/main/java/cryptobot/dto/OrderRecord.java (OrderRecord#adjustStatusByExecutionVolume)

```java
// --- インポート ---
import org.knowm.xchange.dto.Order;
import java.math.BigDecimal;
import java.util.*;
import static java.math.BigDecimal.ZERO;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;
import static org.knowm.xchange.dto.Order.OrderStatus.*;

// --- 所属クラス ---
// class OrderRecord extends LimitOrder

// --- メソッド定義 ---
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
<a id="srcmainjavacryptobotdtoorderrecordjava-orderrecordsetexecutedvolume"></a>
### src/main/java/cryptobot/dto/OrderRecord.java (OrderRecord#setExecutedVolume)

```java
// --- インポート ---
import org.knowm.xchange.dto.Order;
import java.math.BigDecimal;
import java.util.*;
import static org.knowm.xchange.dto.Order.OrderStatus.*;

// --- 所属クラス ---
// class OrderRecord extends LimitOrder

// --- メソッド定義 ---
public void setExecutedVolume(BigDecimal executedVolume) {
	setCumulativeAmount(executedVolume);
	adjustStatusByExecutionVolume(); // -> [OrderRecord.java (adjustStatusByExecutionVolume)](#srcmainjavacryptobotdtoorderrecordjava-orderrecordadjuststatusbyexecutionvolume)
}

```
<a id="srcmainjavacryptobotexchangebitfinexbitfinexjava-bitfinexgetorder"></a>
### src/main/java/cryptobot/exchange/bitfinex/Bitfinex.java (Bitfinex#getOrder)

```java
// --- インポート ---
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import cryptobot.dto.OrderRecord;

// --- 所属クラス ---
// class Bitfinex extends ExchangeService

// --- フィールド ---
private BitfinexTradeService tradeService;

// --- メソッド定義 ---
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
		var openOrders = getOpenOrders(); // -> [Bitfinex.java (getOpenOrders)](#srcmainjavacryptobotexchangebitfinexbitfinexjava-bitfinexgetopenorders)
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
		var origVol = record.getVolume(); // -> [OrderRecord.java (getVolume)](#srcmainjavacryptobotdtoorderrecordjava-orderrecordgetvolume)
		record.setExecutedVolume(execVol); // -> [OrderRecord.java (setExecutedVolume)](#srcmainjavacryptobotdtoorderrecordjava-orderrecordsetexecutedvolume)
		record.setAveragePrice(averagePrice);
		return record;

	}

```
<a id="srcmainjavacryptobotexchangebitflyerbitflyerjava-bitflyertopairstring"></a>
### src/main/java/cryptobot/exchange/bitflyer/Bitflyer.java (Bitflyer#toPairString)

```java
// --- インポート ---
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import java.util.*;

// --- 所属クラス ---
// class Bitflyer extends ExchangeService

// --- メソッド定義 ---
public static String toPairString(CurrencyPair currencyPair) {
	return currencyPair.getBase().toString().toUpperCase() + "_" +
			currencyPair.getCounter().toString().toUpperCase();
}

```
<a id="srcmainjavacryptobotexchangebitflyerbitflyerjava-bitflyerdogetwithauth"></a>
### src/main/java/cryptobot/exchange/bitflyer/Bitflyer.java (Bitflyer#doGetWithAuth)

```java
// --- インポート ---
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import java.util.*;

// --- 所属クラス ---
// class Bitflyer extends ExchangeService

// --- フィールド ---
@Value("${exchange.api.bitflyer:https://api.bitflyer.jp}")
private String URL_BASE;
private static final String ACCESS_KEY = "ACCESS-KEY";
private static final String ACCESS_TIMESTAMP = "ACCESS-TIMESTAMP";
private static final String ACCESS_SIGN = "ACCESS-SIGN";
@Value("${bitflyer.api.key:#{null}}") String API_KEY;
@Value("${bitflyer.api.secret:#{null}}") String SECRET;

// --- メソッド定義 ---
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
<a id="srcmainjavacryptobotexchangebitflyerbitflyerjava-bitflyergetorder"></a>
### src/main/java/cryptobot/exchange/bitflyer/Bitflyer.java (Bitflyer#getOrder)

```java
// --- インポート ---
import cryptobot.dto.OrderRecord;
import cryptobot.exchange.bitflyer.BitflyerOrder.OrderList;
import org.knowm.xchange.dto.trade.LimitOrder;
import java.io.IOException;
import java.util.*;

// --- 所属クラス ---
// class Bitflyer extends ExchangeService

// --- フィールド ---
public static enum OrderFilterBy

// --- メソッド定義 ---
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
<a id="srcmainjavacryptobotexchangebitflyerbitflyerjava-bitflyerparsedate"></a>
### src/main/java/cryptobot/exchange/bitflyer/Bitflyer.java (Bitflyer#parseDate)

```java
// --- インポート ---
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

// --- 所属クラス ---
// class Bitflyer extends ExchangeService

// --- メソッド定義 ---
public static Date parseDate(String datetime) {
	var df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
	try {
		return df.parse(datetime+"+0000");
	} catch (ParseException e) {
		return null;
	}
}

```
<a id="srcmainjavacryptobotexchangebitflyerbitflyerjava-bitflyergettradesbyorderid"></a>
### src/main/java/cryptobot/exchange/bitflyer/Bitflyer.java (Bitflyer#getTradesByOrderId)

```java
// --- インポート ---
import cryptobot.exchange.bitflyer.dto.TradeRecord.TradeList;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.dto.Order.OrderType;
import org.knowm.xchange.dto.trade.UserTrade;
import java.io.IOException;
import java.util.*;
import static java.util.stream.Collectors.toList;
import static org.knowm.xchange.dto.Order.OrderType.ASK;
import static org.knowm.xchange.dto.Order.OrderType.BID;

// --- 所属クラス ---
// class Bitflyer extends ExchangeService

// --- メソッド定義 ---
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
<a id="srcmainjavacryptobotdtoorderrecordjava-orderrecordgetexecutedvolume"></a>
### src/main/java/cryptobot/dto/OrderRecord.java (OrderRecord#getExecutedVolume)

```java
// --- インポート ---
import java.math.BigDecimal;
import java.util.*;
import static org.knowm.xchange.dto.Order.OrderStatus.*;

// --- 所属クラス ---
// class OrderRecord extends LimitOrder

// --- メソッド定義 ---
public BigDecimal getExecutedVolume() {
	return getCumulativeAmount();
}

```
<a id="srcmainjavacryptobotmiscutiljava-utilsleep"></a>
### src/main/java/cryptobot/misc/Util.java (Util#sleep)

```java
// --- インポート ---
import static java.math.BigDecimal.*;
import static java.math.RoundingMode.*;
import static org.knowm.xchange.dto.Order.OrderType.*;

// --- 所属クラス ---
// class Util

// --- メソッド定義 ---
public static void sleep(long milli) {
	try {
		Thread.sleep(milli);
	} catch (InterruptedException e) {
		throw new RuntimeException(e);
	}
}

```
<a id="srcmainjavacryptobotexchangebitflyerbitflyerjava-bitflyergetorder"></a>
### src/main/java/cryptobot/exchange/bitflyer/Bitflyer.java (Bitflyer#getOrder)

```java
// --- インポート ---
import cryptobot.dto.OrderRecord;
import cryptobot.misc.Util;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import static java.math.BigDecimal.ZERO;

// --- 所属クラス ---
// class Bitflyer extends ExchangeService

// --- フィールド ---
public static enum OrderFilterBy

// --- メソッド定義 ---
synchronized public OrderRecord getOrder(String childOrderId) throws IOException {
	var op = orderPool.getById(childOrderId);
	if (op != null) {
		return op;
	}
	var order = getOrder(childOrderId, OrderFilterBy.child_order_id);
	var executedVolume = ZERO;
	// 約定していても注文情報に反映されていない場合があるため、念のため取引情報を取得して確認
	var trades = getTradesByOrderId(childOrderId);
	executedVolume = trades.stream()
		.map(trade->trade.getOriginalAmount()).reduce(ZERO, BigDecimal::add);
	// 注文なし＆取引なしの場合（キャンセル済 or 無効な注文）
	if (order == null && executedVolume.compareTo(ZERO) == 0) {
		return null;
	}
	if (order != null) {
		// 注文情報が取得できた場合 ⇒約定金額によりステータスを決定
		order.setExecutedVolume(order.getExecutedVolume().max(executedVolume));
		return order;
	} else {
		// 注文取得できず、取引情報が¥取得できた場合 ⇒注文情報の反映が遅延していると想定して再取得
		Util.sleep(1000);
		// orderが取得できなくても約定しているはずなのでも再度実行
		return getOrder(childOrderId);
	}
}

```
<a id="srcmainjavacryptobotexchangebitflyerbitflyerfxjava-bitflyerfxcreatehmac"></a>
### src/main/java/cryptobot/exchange/bitflyer/BitflyerFX.java (BitflyerFX#createHMAC)

```java
// --- インポート ---
import static java.math.BigDecimal.*;
import static java.math.RoundingMode.*;
import static org.knowm.xchange.currency.Currency.*;
import static org.knowm.xchange.dto.Order.OrderType.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.binary.Hex;
import org.springframework.beans.factory.annotation.Value;

// --- 所属クラス ---
// class BitflyerFX extends ExchangeService

// --- フィールド ---
private static final String HMAC_SHA256 = "HmacSHA256";
@Value("${bitflyer.api.secret:#{null}}") String SECRET;

// --- メソッド定義 ---
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
<a id="srcmainjavacryptobotexchangebitflyerbitflyerfxjava-bitflyerfxdogetwithauth"></a>
### src/main/java/cryptobot/exchange/bitflyer/BitflyerFX.java (BitflyerFX#doGetWithAuth)

```java
// --- インポート ---
import static java.math.BigDecimal.*;
import static java.math.RoundingMode.*;
import static org.knowm.xchange.currency.Currency.*;
import static org.knowm.xchange.dto.Order.OrderType.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;

// --- 所属クラス ---
// class BitflyerFX extends ExchangeService

// --- フィールド ---
private static final String URL_BASE = "https://api.bitflyer.jp";
private static final String ACCESS_KEY = "ACCESS-KEY";
private static final String ACCESS_TIMESTAMP = "ACCESS-TIMESTAMP";
private static final String ACCESS_SIGN = "ACCESS-SIGN";
@Value("${bitflyer.api.key:#{null}}") String API_KEY;

// --- メソッド定義 ---
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
<a id="srcmainjavacryptobotexchangebitflyerbitflyerfxjava-bitflyerfxgetorder"></a>
### src/main/java/cryptobot/exchange/bitflyer/BitflyerFX.java (BitflyerFX#getOrder)

```java
// --- インポート ---
import static java.math.BigDecimal.*;
import static java.math.RoundingMode.*;
import static org.knowm.xchange.currency.Currency.*;
import static org.knowm.xchange.dto.Order.OrderType.*;
import java.io.IOException;
import java.util.*;
import static java.util.stream.Collectors.joining;
import cryptobot.exchange.bitflyer.BitflyerOrder.OrderList;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.springframework.beans.factory.annotation.Value;
import cryptobot.dto.OrderRecord;

// --- 所属クラス ---
// class BitflyerFX extends ExchangeService

// --- フィールド ---
private static final String ccyps = "FX_BTC_JPY";

// --- メソッド定義 ---
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
<a id="srcmainjavacryptobotdtoorderbookjava-orderbookput"></a>
### src/main/java/cryptobot/dto/Orderbook.java (Orderbook#put)

```java
// --- インポート ---
import static java.math.BigDecimal.*;
import static java.math.RoundingMode.*;
import static org.knowm.xchange.dto.Order.OrderType.*;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.TreeMap;
import org.knowm.xchange.dto.Order.OrderType;

// --- 所属クラス ---
// class Orderbook extends HashMap<OrderType,TreeMap<BigDecimal,BigDecimal>>

// --- メソッド定義 ---
@Override
public TreeMap<BigDecimal, BigDecimal> put(OrderType type, TreeMap<BigDecimal, BigDecimal> values) {
	TreeMap<BigDecimal, BigDecimal> map = type.equals(BID) ?
			new TreeMap<>(Comparator.reverseOrder()) :
			new TreeMap<>();
	map.putAll(values);
	super.put(type, map);
	return map;
}

```
<a id="srcmainjavacryptobotexchangebitpointbitpointjava-bitpointdohttpget"></a>
### src/main/java/cryptobot/exchange/bitpoint/Bitpoint.java (Bitpoint#doHttpGet)

```java
// --- インポート ---
import static java.math.BigDecimal.*;
import static java.util.stream.Collectors.*;
import static org.apache.commons.lang3.StringUtils.*;
import static org.knowm.xchange.currency.CurrencyPair.*;
import static org.knowm.xchange.dto.Order.OrderType.*;
import java.util.HashMap;
import java.util.Map;
import org.knowm.xchange.dto.Order;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.JsonNode;
import cryptobot.misc.JsonUtils;

// --- 所属クラス ---
// class Bitpoint extends ExchangeService

// --- フィールド ---
private static final String URL_BASE = "https://smartapi.bitpoint.co.jp/bpj-smart-api";
private static final String APPLICATION_JSON = "application/json";
private static final String ACCESS_KEY = "access-key";
private static final String ACCESS_NONCE = "access-nonce";
private static final String ACCESS_SIGN = "access-signature";
private static final String CONTENT_TYPE = "Content-Type";
@Value("${bitpoint.api.key:#{null}}") String API_KEY;
@Value("${bitpoint.api.secret:#{null}}") String SECRET;

// --- メソッド定義 ---
private JsonNode doHttpGet(String path, Map<String, String> params) throws Exception {
	long nonce = System.currentTimeMillis();
	var requestParam = new HashMap<>(params);
	requestParam.put("timestamp", String.valueOf(nonce)); // -> [Orderbook.java (put)](#srcmainjavacryptobotdtoorderbookjava-orderbookput)
	requestParam.put("recvWindow", "10000"); // -> [Orderbook.java (put)](#srcmainjavacryptobotdtoorderbookjava-orderbookput)
	
	var paramString = requestParam.entrySet().stream()
			.map(e -> e.getKey() + "=" + e.getValue())
			.collect(joining("&"));
	
	var url = URL_BASE + path + (isEmpty(paramString) ? "" : "?" + paramString);
	var message = API_KEY + "\n" + nonce + "\n" + (paramString == null ? "" : paramString);
	var signature = createHMAC(SECRET, message); // -> [GenericService.java (createHMAC)](#srcmainjavacryptobotexchangegenericservicejava-genericservicecreatehmac)

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
<a id="srcmainjavacryptobotexchangebitpointbitpointjava-bitpointadaptstatus"></a>
### src/main/java/cryptobot/exchange/bitpoint/Bitpoint.java (Bitpoint#adaptStatus)

```java
// --- インポート ---
import static java.math.BigDecimal.*;
import static java.util.stream.Collectors.*;
import static org.apache.commons.lang3.StringUtils.*;
import static org.knowm.xchange.currency.CurrencyPair.*;
import static org.knowm.xchange.dto.Order.OrderType.*;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.Order.OrderStatus;

// --- 所属クラス ---
// class Bitpoint extends ExchangeService

// --- メソッド定義 ---
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
<a id="srcmainjavacryptobotexchangebitpointbitpointjava-bitpointcreatelimitorder"></a>
### src/main/java/cryptobot/exchange/bitpoint/Bitpoint.java (Bitpoint#createLimitOrder)

```java
// --- インポート ---
import static java.math.BigDecimal.*;
import static java.util.stream.Collectors.*;
import static org.apache.commons.lang3.StringUtils.*;
import static org.knowm.xchange.currency.CurrencyPair.*;
import static org.knowm.xchange.dto.Order.OrderType.*;
import java.math.BigDecimal;
import java.util.Date;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.trade.LimitOrder;
import com.fasterxml.jackson.databind.JsonNode;

// --- 所属クラス ---
// class Bitpoint extends ExchangeService

// --- メソッド定義 ---
private LimitOrder createLimitOrder(JsonNode json) {
	var orderId = json.path("orderId").asText();
	var type = json.path("side").asText().equals("SELL") ? ASK : BID;
	var executedVol = new BigDecimal(json.path("executedQty").asText("0"));
	var totalVolume = new BigDecimal(json.path("orderQty").asText("0"));
	var price = new BigDecimal(json.path("price").asText("0"));
	var timestamp = new Date(json.path("time").asLong());
	var status = adaptStatus(json.path("status").asText()); // -> [Bitpoint.java (adaptStatus)](#srcmainjavacryptobotexchangebitpointbitpointjava-bitpointadaptstatus)
	var symbol = json.path("symbol").asText("");
	var ccyp = new CurrencyPair(symbol.substring(0, 3), symbol.substring(3, 6));
	return new LimitOrder(type, totalVolume, ccyp, orderId,
			timestamp, price, null, executedVol, ZERO, status);
}

```
<a id="srcmainjavacryptobotexchangebitpointbitpointjava-bitpointgetorder"></a>
### src/main/java/cryptobot/exchange/bitpoint/Bitpoint.java (Bitpoint#getOrder)

```java
// --- インポート ---
import static java.math.BigDecimal.*;
import static java.util.stream.Collectors.*;
import static org.apache.commons.lang3.StringUtils.*;
import static org.knowm.xchange.currency.CurrencyPair.*;
import static org.knowm.xchange.dto.Order.OrderType.*;
import java.util.Map;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.trade.LimitOrder;
import com.fasterxml.jackson.databind.JsonNode;
import cryptobot.dto.OrderRecord;

// --- 所属クラス ---
// class Bitpoint extends ExchangeService

// --- フィールド ---
static Map<CurrencyPair, String> SUPPORTED_CCYP = Map.of(
		CurrencyPair.BTC_JPY, "BTCJPY",
		CurrencyPair.ETH_JPY, "ETHJPY");

// --- メソッド定義 ---
@Override
public OrderRecord getOrder(String id) throws Exception {
	var path = "/api/singleOrder";
	var params = Map.of(
			"symbol", SUPPORTED_CCYP.get(BTC_JPY),
			"orderId", id,
			"tradeType", "SPOT");
	JsonNode json = doHttpGet(path, params); // -> [Bitpoint.java (doHttpGet)](#srcmainjavacryptobotexchangebitpointbitpointjava-bitpointdohttpget)
	log.info("order info : {}", json);
	var order = createLimitOrder(json); // -> [Bitpoint.java (createLimitOrder)](#srcmainjavacryptobotexchangebitpointbitpointjava-bitpointcreatelimitorder)
	return new OrderRecord(this, order);
}

```
<a id="srcmainjavacryptobotexchangeexchangeservicejava-exchangeservicebuyspot"></a>
### src/main/java/cryptobot/exchange/ExchangeService.java (ExchangeService#buySpot)

```java
// --- インポート ---
import static java.math.BigDecimal.*;
import static java.math.RoundingMode.*;
import static java.util.Optional.*;
import static java.util.stream.Collectors.*;
import static org.knowm.xchange.dto.Order.OrderStatus.*;
import static org.knowm.xchange.dto.Order.OrderType.*;
import java.math.BigDecimal;
import java.util.*;
import cryptobot.dto.*;
import org.knowm.xchange.BaseExchange;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.Order.OrderType;
import org.knowm.xchange.dto.marketdata.Trade;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.knowm.xchange.service.trade.TradeService;
import org.springframework.beans.factory.annotation.Value;

// --- 所属クラス ---
// class ExchangeService extends GenericService

// --- フィールド ---
@Value("${cryptobot.currencyPair}") protected CurrencyPair ccyp;
protected BaseExchange xchange;

// --- メソッド定義 ---
public OrderRecord buySpot(BigDecimal volume, BigDecimal limitPrice) throws Exception {
	LimitOrder order = new LimitOrder(OrderType.BID, volume, ccyp, null, null, limitPrice);
	String id = xchange.getTradeService().placeLimitOrder(order);
	return getOrder(id);
}

```
<a id="srcmainjavacryptobotexchangeexchangeservicejava-exchangeservicegetfeerate"></a>
### src/main/java/cryptobot/exchange/ExchangeService.java (ExchangeService#getFeeRate)

```java
// --- インポート ---
import static java.math.BigDecimal.*;
import static java.math.RoundingMode.*;
import static java.util.Optional.*;
import static java.util.stream.Collectors.*;
import static org.knowm.xchange.dto.Order.OrderStatus.*;
import static org.knowm.xchange.dto.Order.OrderType.*;
import java.math.BigDecimal;
import java.util.*;
import cryptobot.dto.*;
import org.knowm.xchange.dto.account.Fee;

// --- 所属クラス ---
// class ExchangeService extends GenericService

// --- フィールド ---
protected Fee fee = new Fee(ZERO, ZERO);
public static enum FeeType

// --- メソッド定義 ---
public BigDecimal getFeeRate(FeeType feeType) {
	if (feeType.equals(FeeType.MAKER)) {
		return fee.getMakerFee();
	} else {
		return fee.getTakerFee();
	}
}

```
<a id="srcmainjavacryptobotexchangeexchangeservicejava-orderpoolwaitorder"></a>
### src/main/java/cryptobot/exchange/ExchangeService.java (OrderPool#waitOrder)

```java
// --- インポート ---
import static java.math.BigDecimal.*;
import static java.math.RoundingMode.*;
import static java.util.Optional.*;
import static java.util.stream.Collectors.*;
import static org.knowm.xchange.dto.Order.OrderStatus.*;
import static org.knowm.xchange.dto.Order.OrderType.*;
import java.util.*;
import cryptobot.dto.*;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.Order.OrderStatus;

// --- 所属クラス ---
// class OrderPool extends ConcurrentHashMap<String,OrderRecord>

// --- フィールド ---
final Set<String> expectedOrderId = new HashSet<>();

// --- メソッド定義 ---
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
<a id="srcmainjavacryptobotexchangeexchangeservicejava-orderpoolupdate"></a>
### src/main/java/cryptobot/exchange/ExchangeService.java (OrderPool#update)

```java
// --- インポート ---
import static java.math.BigDecimal.*;
import static java.math.RoundingMode.*;
import static java.util.Optional.*;
import static java.util.stream.Collectors.*;
import static org.knowm.xchange.dto.Order.OrderStatus.*;
import static org.knowm.xchange.dto.Order.OrderType.*;
import java.util.*;
import cryptobot.dto.*;
import org.knowm.xchange.dto.Order;

// --- 所属クラス ---
// class OrderPool extends ConcurrentHashMap<String,OrderRecord>

// --- フィールド ---
final Set<String> expectedOrderId = new HashSet<>();

// --- メソッド定義 ---
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
<a id="srcmainjavacryptobotexchangeexchangeservicejava-exchangeservicewaitordercreate"></a>
### src/main/java/cryptobot/exchange/ExchangeService.java (ExchangeService#waitOrderCreate)

```java
// --- インポート ---
import static java.math.BigDecimal.*;
import static java.math.RoundingMode.*;
import static java.util.Optional.*;
import static java.util.stream.Collectors.*;
import static org.knowm.xchange.dto.Order.OrderStatus.*;
import static org.knowm.xchange.dto.Order.OrderType.*;
import java.util.*;
import cryptobot.dto.*;
import org.knowm.xchange.dto.Order;

// --- 所属クラス ---
// class ExchangeService extends GenericService

// --- フィールド ---
protected final OrderPool orderPool = new OrderPool();

// --- メソッド定義 ---
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
<a id="srcmainjavacryptobotexchangebinancebinancejava-binancebuyspot"></a>
### src/main/java/cryptobot/exchange/binance/Binance.java (Binance#buySpot)

```java
// --- インポート ---
import cryptobot.dto.OrderRecord;
import cryptobot.exchange.ExchangeService;
import org.knowm.xchange.binance.service.BinanceTradeService;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.Order.OrderType;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.knowm.xchange.dto.trade.LimitOrder.Builder;
import org.knowm.xchange.service.trade.TradeService;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.*;
import static cryptobot.exchange.ExchangeService.FeeType.MAKER;
import static java.math.BigDecimal.ONE;
import static java.math.RoundingMode.HALF_UP;
import static org.knowm.xchange.dto.Order.OrderType.BID;

// --- 所属クラス ---
// class Binance extends ExchangeService

// --- フィールド ---
BinanceTradeService tradeService;
private final int stepSize = 4;

// --- メソッド定義 ---
@Override
synchronized public OrderRecord buySpot(BigDecimal volume, BigDecimal limitPrice) throws Exception {
	// 取得通貨で手数料を徴収されるため、取引金額(volume)に手数分を上乗せて受け渡す。
	volume = volume.divide(ONE.subtract(getFeeRate(MAKER)), stepSize, HALF_UP); // -> [ExchangeService.java (getFeeRate)](#srcmainjavacryptobotexchangeexchangeservicejava-exchangeservicegetfeerate)
	LimitOrder limitOrder = new Builder(OrderType.BID, ccyp)
			.limitPrice(limitPrice).originalAmount(volume.setScale(stepSize, HALF_UP)).build();
	String orderId = tradeService.placeLimitOrder(limitOrder);
	log.info("order sent. id: {}", orderId);
	return waitOrderCreate(orderId); // -> [ExchangeService.java (waitOrderCreate)](#srcmainjavacryptobotexchangeexchangeservicejava-exchangeservicewaitordercreate)
}

```
<a id="srcmainjavacryptobotexchangebitbankbitbankjava-bitbankgetprivaterequestheader"></a>
### src/main/java/cryptobot/exchange/bitbank/Bitbank.java (Bitbank#getPrivateRequestHeader)

```java
// --- インポート ---
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
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import cryptobot.exchange.bitbank.dto.*;

// --- 所属クラス ---
// class Bitbank extends ExchangeService

// --- フィールド ---
@Value("${bitbank.api.secret}") String SECRET;

// --- メソッド定義 ---
protected Map<String, String> getPrivateRequestHeader(String json) {
	long nonce = currentTimeMillis();
	String message = String.valueOf(nonce) + json;
	return makePrivateRequestHeaders(nonce, createHMAC(SECRET, message)); // -> [Bitbank.java (makePrivateRequestHeaders)](#srcmainjavacryptobotexchangebitbankbitbankjava-bitbankmakeprivaterequestheaders), [GenericService.java (createHMAC)](#srcmainjavacryptobotexchangegenericservicejava-genericservicecreatehmac)
}

```
<a id="srcmainjavacryptobotexchangebitbankbitbankjava-bitbankdohttppost"></a>
### src/main/java/cryptobot/exchange/bitbank/Bitbank.java (Bitbank#doHttpPost)

```java
// --- インポート ---
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
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import com.fasterxml.jackson.databind.JsonNode;
import cryptobot.exchange.bitbank.dto.*;
import cryptobot.misc.JsonUtils;

// --- 所属クラス ---
// class Bitbank extends ExchangeService

// --- フィールド ---
@Value("${exchange.api.bitbank:https://api.bitbank.cc}")
private String API_ENDPOINT;

// --- メソッド定義 ---
protected <T> T doHttpPost(String path, Class<T> clazz, Object body) throws Exception {
	String json = JsonUtils.MAPPER.writeValueAsString(body);
	Map<String, String> headers = getPrivateRequestHeader(json); // -> [Bitbank.java (getPrivateRequestHeader)](#srcmainjavacryptobotexchangebitbankbitbankjava-bitbankgetprivaterequestheader)

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
<a id="srcmainjavacryptobotexchangeexchangeservicejava-orderpoolgetbyid"></a>
### src/main/java/cryptobot/exchange/ExchangeService.java (OrderPool#getById)

```java
// --- インポート ---
import static java.math.BigDecimal.*;
import static java.math.RoundingMode.*;
import static java.util.Optional.*;
import static java.util.stream.Collectors.*;
import static org.knowm.xchange.dto.Order.OrderStatus.*;
import static org.knowm.xchange.dto.Order.OrderType.*;
import java.util.*;
import cryptobot.dto.*;
import org.knowm.xchange.dto.Order;

// --- 所属クラス ---
// class OrderPool extends ConcurrentHashMap<String,OrderRecord>

// --- メソッド定義 ---
synchronized public OrderRecord getById(String id) {
	return get(id);
}

```
<a id="srcmainjavacryptobotexchangeexchangeservicejava-orderpoolgetbyid"></a>
### src/main/java/cryptobot/exchange/ExchangeService.java (OrderPool#getById)

```java
// --- インポート ---
import static java.math.BigDecimal.*;
import static java.math.RoundingMode.*;
import static java.util.Optional.*;
import static java.util.stream.Collectors.*;
import static org.knowm.xchange.dto.Order.OrderStatus.*;
import static org.knowm.xchange.dto.Order.OrderType.*;
import java.util.*;
import cryptobot.dto.*;
import org.knowm.xchange.dto.Order;

// --- 所属クラス ---
// class OrderPool extends ConcurrentHashMap<String,OrderRecord>

// --- メソッド定義 ---
synchronized public OrderRecord getById(String id) {
	return get(id);
}

```
<a id="srcmainjavacryptobotdtoorderrecordjava-orderrecordisactive"></a>
### src/main/java/cryptobot/dto/OrderRecord.java (OrderRecord#isActive)

```java
// --- インポート ---
import java.util.*;
import static org.knowm.xchange.dto.Order.OrderStatus.*;

// --- 所属クラス ---
// class OrderRecord extends LimitOrder

// --- メソッド定義 ---
public boolean isActive() {
	return getStatus() == null || !getStatus().isFinal();
}

```
<a id="srcmainjavacryptobotexchangeexchangeservicejava-orderpoolwaitorderclosed"></a>
### src/main/java/cryptobot/exchange/ExchangeService.java (OrderPool#waitOrderClosed)

```java
// --- インポート ---
import static java.math.BigDecimal.*;
import static java.math.RoundingMode.*;
import static java.util.Optional.*;
import static java.util.stream.Collectors.*;
import static org.knowm.xchange.dto.Order.OrderStatus.*;
import static org.knowm.xchange.dto.Order.OrderType.*;
import java.util.*;
import cryptobot.dto.*;
import org.knowm.xchange.dto.Order;

// --- 所属クラス ---
// class OrderPool extends ConcurrentHashMap<String,OrderRecord>

// --- フィールド ---
final Set<String> expectedOrderId = new HashSet<>();

// --- メソッド定義 ---
synchronized public OrderRecord waitOrderClosed(String orderId) throws InterruptedException {
	var startTime = System.currentTimeMillis();
	while (System.currentTimeMillis() - startTime < 10*1000L) {
		var order = getById(orderId); // -> [ExchangeService.java (getById)](#srcmainjavacryptobotexchangeexchangeservicejava-orderpoolgetbyid)
		if (order != null && order.getStatus() != null) {
			if (!order.isActive()) { // -> [OrderRecord.java (isActive)](#srcmainjavacryptobotdtoorderrecordjava-orderrecordisactive)
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
<a id="srcmainjavacryptobotexchangeexchangeservicejava-orderpoolwaitorderclosed"></a>
### src/main/java/cryptobot/exchange/ExchangeService.java (OrderPool#waitOrderClosed)

```java
// --- インポート ---
import static java.math.BigDecimal.*;
import static java.math.RoundingMode.*;
import static java.util.Optional.*;
import static java.util.stream.Collectors.*;
import static org.knowm.xchange.dto.Order.OrderStatus.*;
import static org.knowm.xchange.dto.Order.OrderType.*;
import java.util.*;
import cryptobot.dto.*;
import org.knowm.xchange.dto.Order;

// --- 所属クラス ---
// class OrderPool extends ConcurrentHashMap<String,OrderRecord>

// --- フィールド ---
final Set<String> expectedOrderId = new HashSet<>();

// --- メソッド定義 ---
synchronized public OrderRecord waitOrderClosed(String orderId) throws InterruptedException {
	var startTime = System.currentTimeMillis();
	while (System.currentTimeMillis() - startTime < 10*1000L) {
		var order = getById(orderId); // -> [ExchangeService.java (getById)](#srcmainjavacryptobotexchangeexchangeservicejava-orderpoolgetbyid)
		if (order != null && order.getStatus() != null) {
			if (!order.isActive()) { // -> [OrderRecord.java (isActive)](#srcmainjavacryptobotdtoorderrecordjava-orderrecordisactive)
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
<a id="srcmainjavacryptobotdtoorderrecordjava-orderrecordisactive"></a>
### src/main/java/cryptobot/dto/OrderRecord.java (OrderRecord#isActive)

```java
// --- インポート ---
import java.util.*;
import static org.knowm.xchange.dto.Order.OrderStatus.*;

// --- 所属クラス ---
// class OrderRecord extends LimitOrder

// --- メソッド定義 ---
public boolean isActive() {
	return getStatus() == null || !getStatus().isFinal();
}

```
<a id="srcmainjavacryptobotexchangebitbankbitbankjava-bitbankexecuteorder"></a>
### src/main/java/cryptobot/exchange/bitbank/Bitbank.java (Bitbank#executeOrder)

```java
// --- インポート ---
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
import org.springframework.stereotype.Service;
import cryptobot.dto.OrderRecord;
import cryptobot.exchange.ExchangeService;
import cryptobot.exchange.OrderNotFilledException;
import cryptobot.exchange.bitbank.dto.*;

// --- 所属クラス ---
// class Bitbank extends ExchangeService

// --- フィールド ---
String ccyps;

// --- メソッド定義 ---
private OrderRecord executeOrder(BigDecimal volume, BigDecimal price, BitbankOrders.Side side, BitbankOrders.Type type) throws Exception {
	var path = "/v1/user/spot/order";
	var body = BitbankOrderBody.builder()
			.pair(ccyps)
			.amount(volume.toString())
			.price(price)
			.side(side.getCode())
			.type(type.getCode())
			.build();
	
	var order = doHttpPost(path, BitbankOrders.Order.class, body); // -> [Bitbank.java (doHttpPost)](#srcmainjavacryptobotexchangebitbankbitbankjava-bitbankdohttppost)

	log.info("order sent. id: {}", order.orderId);
	var orderId = String.valueOf(order.orderId);
	
	if (type == BitbankOrders.Type.MARKET) {
		var startTime = currentTimeMillis();
		while (currentTimeMillis() - startTime < 3600 * 1000L) {
			OrderRecord or = waitOrderClosed(orderId); // -> [ExchangeService.java (waitOrderClosed)](#srcmainjavacryptobotexchangeexchangeservicejava-orderpoolwaitorderclosed)
			if (or != null && !or.isActive()) return or; // -> [OrderRecord.java (isActive)](#srcmainjavacryptobotdtoorderrecordjava-orderrecordisactive)
		}
		throw new OrderNotFilledException(orderPool.getById(orderId));
	} else {
		return waitOrderCreate(orderId); // -> [ExchangeService.java (waitOrderCreate)](#srcmainjavacryptobotexchangeexchangeservicejava-exchangeservicewaitordercreate)
	}
}

```
<a id="srcmainjavacryptobotexchangebitbankbitbankjava-bitbankbuyspot"></a>
### src/main/java/cryptobot/exchange/bitbank/Bitbank.java (Bitbank#buySpot)

```java
// --- インポート ---
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
import cryptobot.dto.OrderRecord;
import cryptobot.exchange.bitbank.dto.*;

// --- 所属クラス ---
// class Bitbank extends ExchangeService

// --- メソッド定義 ---
@Override
synchronized public OrderRecord buySpot(BigDecimal volume, BigDecimal limitPrice) throws Exception {
	return executeOrder(volume, limitPrice, BitbankOrders.Side.BUY, BitbankOrders.Type.LIMIT); // -> [Bitbank.java (executeOrder)](#srcmainjavacryptobotexchangebitbankbitbankjava-bitbankexecuteorder)
}

```
<a id="srcmainjavacryptobotexchangeexchangeservicejava-exchangeservicebuyspot"></a>
### src/main/java/cryptobot/exchange/ExchangeService.java (ExchangeService#buySpot)

```java
// --- インポート ---
import static java.math.BigDecimal.*;
import static java.math.RoundingMode.*;
import static java.util.Optional.*;
import static java.util.stream.Collectors.*;
import static org.knowm.xchange.dto.Order.OrderStatus.*;
import static org.knowm.xchange.dto.Order.OrderType.*;
import java.math.BigDecimal;
import java.util.*;
import cryptobot.dto.*;
import org.knowm.xchange.BaseExchange;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.Order.OrderType;
import org.knowm.xchange.dto.marketdata.Trade;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.knowm.xchange.service.trade.TradeService;
import org.springframework.beans.factory.annotation.Value;

// --- 所属クラス ---
// class ExchangeService extends GenericService

// --- フィールド ---
@Value("${cryptobot.currencyPair}") protected CurrencyPair ccyp;
protected BaseExchange xchange;

// --- メソッド定義 ---
public OrderRecord buySpot(BigDecimal volume, BigDecimal limitPrice) throws Exception {
	LimitOrder order = new LimitOrder(OrderType.BID, volume, ccyp, null, null, limitPrice);
	String id = xchange.getTradeService().placeLimitOrder(order);
	return getOrder(id);
}

```
<a id="srcmainjavacryptobotexchangebitfinexbitfinexjava-bitfinexbuyspot"></a>
### src/main/java/cryptobot/exchange/bitfinex/Bitfinex.java (Bitfinex#buySpot)

```java
// --- インポート ---
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
import org.knowm.xchange.Exchange;
import org.knowm.xchange.bitfinex.service.BitfinexTradeService;
import org.knowm.xchange.bitfinex.v1.BitfinexOrderType;
import org.knowm.xchange.bitfinex.v1.dto.trade.BitfinexOrderStatusResponse;
import org.knowm.xchange.dto.Order.OrderStatus;
import org.knowm.xchange.dto.Order.OrderType;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.springframework.stereotype.Service;
import cryptobot.dto.OrderRecord;
import cryptobot.exchange.ExchangeService;

// --- 所属クラス ---
// class Bitfinex extends ExchangeService

// --- フィールド ---
private BitfinexTradeService tradeService;

// --- メソッド定義 ---
@Override
public OrderRecord buySpot(BigDecimal volume, BigDecimal limitPrice) throws Exception {
	// 取得通貨で手数料を徴収されるため、取引金額(volume)に手数分を上乗せて受け渡す。
	volume = volume.divide(ONE.subtract(getFeeRate(MAKER)), // -> [ExchangeService.java (getFeeRate)](#srcmainjavacryptobotexchangeexchangeservicejava-exchangeservicegetfeerate)
			8, RoundingMode.HALF_UP);
	LimitOrder limitOrder = new LimitOrder.Builder(OrderType.BID, ccyp)
			.limitPrice(limitPrice).originalAmount(volume).build();
	BitfinexOrderStatusResponse newOrder = tradeService.placeBitfinexLimitOrder(
			limitOrder, BitfinexOrderType.LIMIT);
	log.info("order executed. id: {}", newOrder);
	return waitOrderCreate(String.valueOf(newOrder.getId())); // -> [ExchangeService.java (waitOrderCreate)](#srcmainjavacryptobotexchangeexchangeservicejava-exchangeservicewaitordercreate)

}

```
<a id="srcmainjavacryptobotexchangebitflyerbitflyerjava-bitflyerdopostwithauth"></a>
### src/main/java/cryptobot/exchange/bitflyer/Bitflyer.java (Bitflyer#doPostWithAuth)

```java
// --- インポート ---
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import java.util.*;

// --- 所属クラス ---
// class Bitflyer extends ExchangeService

// --- フィールド ---
@Value("${exchange.api.bitflyer:https://api.bitflyer.jp}")
private String URL_BASE;
private static final String APPLICATION_JSON = "application/json";
private static final String ACCESS_KEY = "ACCESS-KEY";
private static final String ACCESS_TIMESTAMP = "ACCESS-TIMESTAMP";
private static final String ACCESS_SIGN = "ACCESS-SIGN";
@Value("${bitflyer.api.key:#{null}}") String API_KEY;
@Value("${bitflyer.api.secret:#{null}}") String SECRET;

// --- メソッド定義 ---
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
<a id="srcmainjavacryptobotexchangebitflyerdtoorderresultjava-orderresultgetchildorderacceptanceid"></a>
### src/main/java/cryptobot/exchange/bitflyer/dto/OrderResult.java (OrderResult#getChildOrderAcceptanceId)

```java
// --- インポート ---
import com.fasterxml.jackson.annotation.JsonProperty;

// --- 所属クラス ---
// class OrderResult

// --- フィールド ---
@JsonProperty("child_order_acceptance_id")
private String childOrderAcceptanceId;

// --- メソッド定義 ---
public String getChildOrderAcceptanceId() {
	return childOrderAcceptanceId;
}

```
<a id="srcmainjavacryptobotexchangebitflyerbitflyerjava-bitflyerplacelimitorder"></a>
### src/main/java/cryptobot/exchange/bitflyer/Bitflyer.java (Bitflyer#placeLimitOrder)

```java
// --- インポート ---
import com.fasterxml.jackson.databind.ObjectMapper;
import cryptobot.exchange.bitflyer.dto.BitflyerNewOrderRequest;
import cryptobot.exchange.bitflyer.dto.OrderResult;
import cryptobot.misc.JsonUtils;
import cryptobot.misc.Util;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order.OrderType;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.springframework.beans.factory.annotation.Value;
import java.util.*;

// --- 所属クラス ---
// class Bitflyer extends ExchangeService

// --- フィールド ---
private static final ObjectMapper OBJECT_MAPPER = JsonUtils.MAPPER;
public enum BitflyerOrderType

// --- メソッド定義 ---
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
<a id="srcmainjavacryptobotexchangebitflyerbitflyerjava-bitflyergetorderwithretry"></a>
### src/main/java/cryptobot/exchange/bitflyer/Bitflyer.java (Bitflyer#getOrderWithRetry)

```java
// --- インポート ---
import cryptobot.dto.OrderRecord;
import cryptobot.exchange.PlacedOrderNotFoundException;
import java.util.*;
import static cryptobot.exchange.bitflyer.Bitflyer.OrderFilterBy.child_order_acceptance_id;

// --- 所属クラス ---
// class Bitflyer extends ExchangeService

// --- メソッド定義 ---
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
<a id="srcmainjavacryptobotexchangebitflyerbitflyerjava-bitflyerwaitordercreate"></a>
### src/main/java/cryptobot/exchange/bitflyer/Bitflyer.java (Bitflyer#waitOrderCreate)

```java
// --- インポート ---
import cryptobot.dto.OrderRecord;
import java.util.*;

// --- 所属クラス ---
// class Bitflyer extends ExchangeService

// --- メソッド定義 ---
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
<a id="srcmainjavacryptobotexchangebitflyerbitflyerjava-bitflyerbuyspot"></a>
### src/main/java/cryptobot/exchange/bitflyer/Bitflyer.java (Bitflyer#buySpot)

```java
// --- インポート ---
import cryptobot.dto.OrderRecord;
import org.knowm.xchange.dto.Order.OrderType;
import org.knowm.xchange.dto.account.Fee;
import org.knowm.xchange.dto.trade.LimitOrder;
import java.math.BigDecimal;
import java.util.*;
import static java.math.BigDecimal.ONE;
import static java.math.RoundingMode.HALF_UP;
import static org.knowm.xchange.dto.Order.OrderType.BID;

// --- 所属クラス ---
// class Bitflyer extends ExchangeService

// --- メソッド定義 ---
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
<a id="srcmainjavacryptobotexchangebitflyerbitflyerfxjava-bitflyerfxbuyspot"></a>
### src/main/java/cryptobot/exchange/bitflyer/BitflyerFX.java (BitflyerFX#buySpot)

```java
// --- インポート ---
import static java.math.BigDecimal.*;
import static java.math.RoundingMode.*;
import static org.knowm.xchange.currency.Currency.*;
import static org.knowm.xchange.dto.Order.OrderType.*;
import java.math.BigDecimal;
import java.util.*;
import cryptobot.dto.OrderRecord;

// --- 所属クラス ---
// class BitflyerFX extends ExchangeService

// --- フィールド ---
public BigDecimal volume;

// --- メソッド定義 ---
@Override
public OrderRecord buySpot(BigDecimal volume, BigDecimal limitPrice) throws Exception {
	return null;
}

```
<a id="srcmainjavacryptobotexchangebitpointbitpointjava-bitpointdohttppost"></a>
### src/main/java/cryptobot/exchange/bitpoint/Bitpoint.java (Bitpoint#doHttpPost)

```java
// --- インポート ---
import static java.math.BigDecimal.*;
import static java.util.stream.Collectors.*;
import static org.apache.commons.lang3.StringUtils.*;
import static org.knowm.xchange.currency.CurrencyPair.*;
import static org.knowm.xchange.dto.Order.OrderType.*;
import java.util.HashMap;
import java.util.Map;
import org.knowm.xchange.dto.Order;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.JsonNode;
import cryptobot.misc.JsonUtils;

// --- 所属クラス ---
// class Bitpoint extends ExchangeService

// --- フィールド ---
private static final String URL_BASE = "https://smartapi.bitpoint.co.jp/bpj-smart-api";
private static final String APPLICATION_JSON = "application/json";
private static final String ACCESS_KEY = "access-key";
private static final String ACCESS_NONCE = "access-nonce";
private static final String ACCESS_SIGN = "access-signature";
private static final String CONTENT_TYPE = "Content-Type";
@Value("${bitpoint.api.key:#{null}}") String API_KEY;
@Value("${bitpoint.api.secret:#{null}}") String SECRET;

// --- メソッド定義 ---
private JsonNode doHttpPost(String path, Map<String, String> params) throws Exception {
	long nonce = System.currentTimeMillis();
	var data = new HashMap<>(params);
	data.put("timestamp", String.valueOf(nonce)); // -> [Orderbook.java (put)](#srcmainjavacryptobotdtoorderbookjava-orderbookput)
	data.put("recvWindow", "10000"); // -> [Orderbook.java (put)](#srcmainjavacryptobotdtoorderbookjava-orderbookput)
	
	var json = JsonUtils.MAPPER.writeValueAsString(data);
	var url = URL_BASE + path;
	var message = API_KEY + "\n" + nonce + "\n" + (json == null ? "" : json);
	var signature = createHMAC(SECRET, message); // -> [GenericService.java (createHMAC)](#srcmainjavacryptobotexchangegenericservicejava-genericservicecreatehmac)

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
<a id="srcmainjavacryptobotexchangebitpointbitpointjava-bitpointbuyspot"></a>
### src/main/java/cryptobot/exchange/bitpoint/Bitpoint.java (Bitpoint#buySpot)

```java
// --- インポート ---
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
import org.springframework.beans.factory.annotation.Value;
import com.fasterxml.jackson.databind.JsonNode;
import cryptobot.dto.OrderRecord;

// --- 所属クラス ---
// class Bitpoint extends ExchangeService

// --- フィールド ---
@Value("${bitpoint.pincode:#{null}}") String PIN_CODE;
static Map<CurrencyPair, String> SUPPORTED_CCYP = Map.of(
		CurrencyPair.BTC_JPY, "BTCJPY",
		CurrencyPair.ETH_JPY, "ETHJPY");

// --- メソッド定義 ---
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
<a id="srcmainjavacryptobotexchangeexchangeservicejava-exchangeservicesellspot"></a>
### src/main/java/cryptobot/exchange/ExchangeService.java (ExchangeService#sellSpot)

```java
// --- インポート ---
import static java.math.BigDecimal.*;
import static java.math.RoundingMode.*;
import static java.util.Optional.*;
import static java.util.stream.Collectors.*;
import static org.knowm.xchange.dto.Order.OrderStatus.*;
import static org.knowm.xchange.dto.Order.OrderType.*;
import java.math.BigDecimal;
import java.util.*;
import cryptobot.dto.*;
import org.knowm.xchange.BaseExchange;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.Order.OrderType;
import org.knowm.xchange.dto.marketdata.Trade;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.knowm.xchange.service.trade.TradeService;
import org.springframework.beans.factory.annotation.Value;

// --- 所属クラス ---
// class ExchangeService extends GenericService

// --- フィールド ---
@Value("${cryptobot.currencyPair}") protected CurrencyPair ccyp;
protected BaseExchange xchange;

// --- メソッド定義 ---
public OrderRecord sellSpot(BigDecimal volume, BigDecimal limitPrice) throws Exception {
	LimitOrder order = new LimitOrder(OrderType.ASK, volume, ccyp, null, null, limitPrice);
	String id = xchange.getTradeService().placeLimitOrder(order);
	return getOrder(id);
}

```
<a id="srcmainjavacryptobotexchangebinancebinancejava-binancesellspot"></a>
### src/main/java/cryptobot/exchange/binance/Binance.java (Binance#sellSpot)

```java
// --- インポート ---
import cryptobot.dto.OrderRecord;
import cryptobot.exchange.ExchangeService;
import org.knowm.xchange.binance.service.BinanceTradeService;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.Order.OrderType;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.knowm.xchange.dto.trade.LimitOrder.Builder;
import org.knowm.xchange.service.trade.TradeService;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.*;
import static java.math.RoundingMode.HALF_UP;
import static org.knowm.xchange.dto.Order.OrderType.ASK;

// --- 所属クラス ---
// class Binance extends ExchangeService

// --- フィールド ---
BinanceTradeService tradeService;
private final int stepSize = 4;

// --- メソッド定義 ---
@Override
synchronized public OrderRecord sellSpot(BigDecimal volume, BigDecimal limitPrice) throws Exception {
	LimitOrder limitOrder = new Builder(OrderType.ASK, ccyp)
			.limitPrice(limitPrice).originalAmount(volume.setScale(stepSize, HALF_UP)).build();
	String orderId = tradeService.placeLimitOrder(limitOrder);
	log.info("order sent. id: {}", orderId);
	return waitOrderCreate(orderId); // -> [ExchangeService.java (waitOrderCreate)](#srcmainjavacryptobotexchangeexchangeservicejava-exchangeservicewaitordercreate)
}

```
<a id="srcmainjavacryptobotexchangebitbankbitbankjava-bitbanksellspot"></a>
### src/main/java/cryptobot/exchange/bitbank/Bitbank.java (Bitbank#sellSpot)

```java
// --- インポート ---
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
import cryptobot.dto.OrderRecord;
import cryptobot.exchange.bitbank.dto.*;

// --- 所属クラス ---
// class Bitbank extends ExchangeService

// --- メソッド定義 ---
@Override
synchronized public OrderRecord sellSpot(BigDecimal volume, BigDecimal limitPrice) throws Exception {
	return executeOrder(volume, limitPrice, BitbankOrders.Side.SELL, BitbankOrders.Type.LIMIT); // -> [Bitbank.java (executeOrder)](#srcmainjavacryptobotexchangebitbankbitbankjava-bitbankexecuteorder)
}

```
<a id="srcmainjavacryptobotexchangeexchangeservicejava-exchangeservicesellspot"></a>
### src/main/java/cryptobot/exchange/ExchangeService.java (ExchangeService#sellSpot)

```java
// --- インポート ---
import static java.math.BigDecimal.*;
import static java.math.RoundingMode.*;
import static java.util.Optional.*;
import static java.util.stream.Collectors.*;
import static org.knowm.xchange.dto.Order.OrderStatus.*;
import static org.knowm.xchange.dto.Order.OrderType.*;
import java.math.BigDecimal;
import java.util.*;
import cryptobot.dto.*;
import org.knowm.xchange.BaseExchange;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.Order.OrderType;
import org.knowm.xchange.dto.marketdata.Trade;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.knowm.xchange.service.trade.TradeService;
import org.springframework.beans.factory.annotation.Value;

// --- 所属クラス ---
// class ExchangeService extends GenericService

// --- フィールド ---
@Value("${cryptobot.currencyPair}") protected CurrencyPair ccyp;
protected BaseExchange xchange;

// --- メソッド定義 ---
public OrderRecord sellSpot(BigDecimal volume, BigDecimal limitPrice) throws Exception {
	LimitOrder order = new LimitOrder(OrderType.ASK, volume, ccyp, null, null, limitPrice);
	String id = xchange.getTradeService().placeLimitOrder(order);
	return getOrder(id);
}

```
<a id="srcmainjavacryptobotexchangebitfinexbitfinexjava-bitfinexsellspot"></a>
### src/main/java/cryptobot/exchange/bitfinex/Bitfinex.java (Bitfinex#sellSpot)

```java
// --- インポート ---
import static cryptobot.exchange.ExchangeService.FeeType.*;
import static java.math.BigDecimal.*;
import static java.math.RoundingMode.*;
import static java.util.Objects.*;
import static java.util.stream.Collectors.*;
import static org.knowm.xchange.dto.Order.OrderStatus.*;
import static org.knowm.xchange.dto.Order.OrderType.*;
import java.math.BigDecimal;
import java.util.*;
import org.knowm.xchange.Exchange;
import org.knowm.xchange.bitfinex.service.BitfinexTradeService;
import org.knowm.xchange.bitfinex.v1.BitfinexOrderType;
import org.knowm.xchange.bitfinex.v1.dto.trade.BitfinexOrderStatusResponse;
import org.knowm.xchange.dto.Order.OrderStatus;
import org.knowm.xchange.dto.Order.OrderType;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.springframework.stereotype.Service;
import cryptobot.dto.OrderRecord;
import cryptobot.exchange.ExchangeService;

// --- 所属クラス ---
// class Bitfinex extends ExchangeService

// --- フィールド ---
private BitfinexTradeService tradeService;

// --- メソッド定義 ---
@Override
public OrderRecord sellSpot(BigDecimal volume, BigDecimal limitPrice) throws Exception {
	LimitOrder limitOrder = new LimitOrder.Builder(OrderType.ASK, ccyp)
			.limitPrice(limitPrice).originalAmount(volume).build();
	BitfinexOrderStatusResponse newOrder = tradeService.placeBitfinexLimitOrder(
			limitOrder, BitfinexOrderType.LIMIT);
	log.info("order executed. id: {}", newOrder);
	return waitOrderCreate(String.valueOf(newOrder.getId())); // -> [ExchangeService.java (waitOrderCreate)](#srcmainjavacryptobotexchangeexchangeservicejava-exchangeservicewaitordercreate)
}

```
<a id="srcmainjavacryptobotexchangebitflyerbitflyerjava-bitflyersellspot"></a>
### src/main/java/cryptobot/exchange/bitflyer/Bitflyer.java (Bitflyer#sellSpot)

```java
// --- インポート ---
import cryptobot.dto.OrderRecord;
import org.knowm.xchange.dto.Order.OrderType;
import org.knowm.xchange.dto.account.Fee;
import org.knowm.xchange.dto.trade.LimitOrder;
import java.math.BigDecimal;
import java.util.*;
import static java.math.BigDecimal.ONE;
import static java.math.RoundingMode.HALF_UP;
import static org.knowm.xchange.dto.Order.OrderType.ASK;

// --- 所属クラス ---
// class Bitflyer extends ExchangeService

// --- メソッド定義 ---
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
<a id="srcmainjavacryptobotexchangebitflyerbitflyerfxjava-bitflyerfxsellspot"></a>
### src/main/java/cryptobot/exchange/bitflyer/BitflyerFX.java (BitflyerFX#sellSpot)

```java
// --- インポート ---
import static java.math.BigDecimal.*;
import static java.math.RoundingMode.*;
import static org.knowm.xchange.currency.Currency.*;
import static org.knowm.xchange.dto.Order.OrderType.*;
import java.math.BigDecimal;
import java.util.*;
import cryptobot.dto.OrderRecord;

// --- 所属クラス ---
// class BitflyerFX extends ExchangeService

// --- フィールド ---
public BigDecimal volume;

// --- メソッド定義 ---
@Override
public OrderRecord sellSpot(BigDecimal volume, BigDecimal limitPrice) throws Exception {
	return null;
}

```
<a id="srcmainjavacryptobotexchangebitpointbitpointjava-bitpointsellspot"></a>
### src/main/java/cryptobot/exchange/bitpoint/Bitpoint.java (Bitpoint#sellSpot)

```java
// --- インポート ---
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
import org.springframework.beans.factory.annotation.Value;
import com.fasterxml.jackson.databind.JsonNode;
import cryptobot.dto.OrderRecord;

// --- 所属クラス ---
// class Bitpoint extends ExchangeService

// --- フィールド ---
@Value("${bitpoint.pincode:#{null}}") String PIN_CODE;
static Map<CurrencyPair, String> SUPPORTED_CCYP = Map.of(
		CurrencyPair.BTC_JPY, "BTCJPY",
		CurrencyPair.ETH_JPY, "ETHJPY");

// --- メソッド定義 ---
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
<a id="srcmainjavacryptobotmiscmailsenderjava-mailsendersend"></a>
### src/main/java/cryptobot/misc/MailSender.java (MailSender#send)

```java
// --- インポート ---
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import java.io.UnsupportedEncodingException;
import java.util.Properties;

// --- 所属クラス ---
// class MailSender

// --- フィールド ---
@Value("${mail.smtp.userid}") String userid;
@Value("${mail.smtp.password}") String password;
@Value("${mail.smtp.host}") String hostname;
@Value("${mail.smtp.port:587}") String port;
@Value("${mail.destination}") String destination;

// --- メソッド定義 ---
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
<a id="srcmainjavacryptobotmiscmailsenderjava-mailsenderinfo"></a>
### src/main/java/cryptobot/misc/MailSender.java (MailSender#info)

```java
// --- インポート ---
import jakarta.mail.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// --- 所属クラス ---
// class MailSender

// --- フィールド ---
ExecutorService executor = Executors.newThreadPerTaskExecutor(
        Thread.ofVirtual().name("mail-send").factory());

// --- メソッド定義 ---
public void info(String subject, String body) {
    executor.submit(()->{
        send(subject, body); // -> [MailSender.java (send)](#srcmainjavacryptobotmiscmailsenderjava-mailsendersend)
    });
}

```
<a id="srcmainjavacryptobotexchangeexchangeservicejava-exchangeservicegetname"></a>
### src/main/java/cryptobot/exchange/ExchangeService.java (ExchangeService#getName)

```java
// --- インポート ---
import static java.math.BigDecimal.*;
import static java.math.RoundingMode.*;
import static java.util.Optional.*;
import static java.util.stream.Collectors.*;
import static org.knowm.xchange.dto.Order.OrderStatus.*;
import static org.knowm.xchange.dto.Order.OrderType.*;
import java.util.*;
import cryptobot.dto.*;

// --- 所属クラス ---
// class ExchangeService extends GenericService

// --- メソッド定義 ---
public String getName() {
	return this.getClass().getSimpleName();
}

```
<a id="srcmainjavacryptobotexchangeexchangeservicejava-exchangeservicegetname"></a>
### src/main/java/cryptobot/exchange/ExchangeService.java (ExchangeService#getName)

```java
// --- インポート ---
import static java.math.BigDecimal.*;
import static java.math.RoundingMode.*;
import static java.util.Optional.*;
import static java.util.stream.Collectors.*;
import static org.knowm.xchange.dto.Order.OrderStatus.*;
import static org.knowm.xchange.dto.Order.OrderType.*;
import java.util.*;
import cryptobot.dto.*;

// --- 所属クラス ---
// class ExchangeService extends GenericService

// --- メソッド定義 ---
public String getName() {
	return this.getClass().getSimpleName();
}

```
<a id="srcmainjavacryptobotexchangeexchangeservicejava-exchangeserviceorder"></a>
### src/main/java/cryptobot/exchange/ExchangeService.java (ExchangeService#order)

```java
// --- インポート ---
import static java.math.BigDecimal.*;
import static java.math.RoundingMode.*;
import static java.util.Optional.*;
import static java.util.stream.Collectors.*;
import static org.knowm.xchange.dto.Order.OrderStatus.*;
import static org.knowm.xchange.dto.Order.OrderType.*;
import java.math.BigDecimal;
import java.util.*;
import cryptobot.dto.*;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.Order.OrderType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import cryptobot.misc.MailSender;

// --- 所属クラス ---
// class ExchangeService extends GenericService

// --- フィールド ---
@Value("${cryptobot.price.precision}") protected int scale;
@Value("${cryptobot.volume.precision}") protected int volScale;
protected final Logger log = LoggerFactory.getLogger(this.getClass());
@Autowired MailSender mailer;

// --- メソッド定義 ---
/**
 * 指値注文
 * @param type
 * @param volume
 * @param limitPrice
 * @return
 * @throws Exception
 */
public OrderRecord order(OrderType type, BigDecimal volume, BigDecimal limitPrice) throws Exception {
	OrderRecord order;
	try {
		if (type.equals(OrderType.BID)) {
			order = buySpot(volume.setScale(volScale, HALF_UP), limitPrice.setScale(scale, HALF_UP));
		} else {
			order = sellSpot(volume.setScale(volScale, HALF_UP), limitPrice.setScale(scale, HALF_UP));
		}
		log.info("limit order created. order: {}", order);
		return order;
	} catch (Exception e) {
		mailer.info("exchange disabled on order error", "disabled exchange: " + getName());
		setEnable(false);
		throw e;
	}
}

```
<a id="srcmainjavacryptobotexchangeexchangeservicejava-exchangeserviceorder"></a>
### src/main/java/cryptobot/exchange/ExchangeService.java (ExchangeService#order)

```java
// --- インポート ---
import static java.math.BigDecimal.*;
import static java.math.RoundingMode.*;
import static java.util.Optional.*;
import static java.util.stream.Collectors.*;
import static org.knowm.xchange.dto.Order.OrderStatus.*;
import static org.knowm.xchange.dto.Order.OrderType.*;
import java.math.BigDecimal;
import java.util.*;
import cryptobot.dto.*;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.Order.OrderType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import cryptobot.misc.MailSender;

// --- 所属クラス ---
// class ExchangeService extends GenericService

// --- フィールド ---
@Value("${cryptobot.price.precision}") protected int scale;
@Value("${cryptobot.volume.precision}") protected int volScale;
protected final Logger log = LoggerFactory.getLogger(this.getClass());
@Autowired MailSender mailer;

// --- メソッド定義 ---
/**
 * 指値注文
 * @param type
 * @param volume
 * @param limitPrice
 * @return
 * @throws Exception
 */
public OrderRecord order(OrderType type, BigDecimal volume, BigDecimal limitPrice) throws Exception {
	OrderRecord order;
	try {
		if (type.equals(OrderType.BID)) {
			order = buySpot(volume.setScale(volScale, HALF_UP), limitPrice.setScale(scale, HALF_UP));
		} else {
			order = sellSpot(volume.setScale(volScale, HALF_UP), limitPrice.setScale(scale, HALF_UP));
		}
		log.info("limit order created. order: {}", order);
		return order;
	} catch (Exception e) {
		mailer.info("exchange disabled on order error", "disabled exchange: " + getName());
		setEnable(false);
		throw e;
	}
}

```
<a id="srcmainjavacryptobotwebviewpagesmarketviewjava-marketviewplaceorder"></a>
### src/main/java/cryptobot/web/view/pages/MarketView.java (MarketView#placeOrder)

```java
// --- インポート ---
import com.vaadin.flow.component.*;
import cryptobot.exchange.ExchangeService;
import org.knowm.xchange.dto.Order.OrderType;
import java.math.BigDecimal;
import java.util.concurrent.ExecutorService;
import static cryptobot.web.view.pages.CommonComponent.*;
import static java.lang.String.format;

// --- 所属クラス ---
// class MarketView extends VerticalLayout

// --- フィールド ---
ExecutorService taskWorker;

// --- メソッド定義 ---
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
            service.order(orderType, volume, price); // -> [ExchangeService.java (order)](#srcmainjavacryptobotexchangeexchangeservicejava-exchangeserviceorder)
            notifySuccess(this.getUI(),"Order placed");
        } catch (Exception e) {
            notifyError(this.getUI(),"Order error\n%s".formatted(e.getMessage()));
        }
    });
    return true;
}

```
<a id="srcmainjavacryptobotwebviewpagesmarketviewjava-marketviewneworder"></a>
### src/main/java/cryptobot/web/view/pages/MarketView.java (MarketView#newOrder)

```java
// --- インポート ---
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.NumberField;
import cryptobot.exchange.ExchangeService;
import cryptobot.web.entity.Entity.MarketData;
import org.knowm.xchange.dto.Order.OrderType;
import org.springframework.beans.factory.annotation.Value;
import java.math.BigDecimal;
import java.util.List;
import static cryptobot.web.view.pages.CommonComponent.*;
import static java.math.BigDecimal.ONE;
import static org.knowm.xchange.dto.Order.OrderType.ASK;
import static org.knowm.xchange.dto.Order.OrderType.BID;

// --- 所属クラス ---
// class MarketView extends VerticalLayout

// --- フィールド ---
@Value("${cryptobot.price.precision}")  int scale;
@Value("${cryptobot.volume.precision}") int volScale;
private Grid<MarketData> grid;
private Dialog dialog;

// --- メソッド定義 ---
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
        boolean success = placeOrder( // -> [MarketView.java (placeOrder)](#srcmainjavacryptobotwebviewpagesmarketviewjava-marketviewplaceorder)
            marketData.getExchangeService(), orderType.getValue(),
            new BigDecimal(limitPrice.getValue()), new BigDecimal(volume.getValue()));
        if (success) dialog.close();
    });
    execute.addThemeVariants(ButtonVariant.LUMO_ERROR);
    dialog.getFooter().add(cancel, execute);
    dialog.open();
}

```

