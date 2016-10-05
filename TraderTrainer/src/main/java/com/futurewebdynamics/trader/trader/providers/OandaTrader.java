package com.futurewebdynamics.trader.trader.providers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.futurewebdynamics.trader.common.RestHelper;
import com.futurewebdynamics.trader.positions.Position;
import com.futurewebdynamics.trader.positions.PositionStatus;
import com.futurewebdynamics.trader.positions.PositionsManager;
import com.futurewebdynamics.trader.sellconditions.ISellConditionProvider;
import com.futurewebdynamics.trader.trader.ITrader;
import com.futurewebdynamics.trader.trader.providers.Oanda.data.Prices;
import com.futurewebdynamics.trader.trader.providers.Oanda.data.Trade;
import com.futurewebdynamics.trader.trader.providers.Oanda.data.Trades;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.Calendar;
import java.util.Collection;

/**
 * Created by Charlie on 22/08/2016.
 */
public class OandaTrader implements ITrader {

    final static Logger logger = Logger.getLogger(OandaTrader.class);

    private String baseUrl;
    private String accountId;
    private String token;

    private Accounts accounts;

    @Override
    public boolean openPosition(Position position, boolean isShortTrade) {

        String marketOrderJson = "{ \"order\": {" +
                "\"type\":\"MARKET\"," +
                "\"instrument\":\"BCO_USD\"," +
                "\"units\":\"" + (isShortTrade? "-": "") + "100\"," +
                "\"timeInForce\":\"FOK\"," +
                "\"positionFill\":\"OPEN_ONLY\"} }";

        logger.debug("Creating order: " + marketOrderJson);

        try {
            String orderResponseJson = RestHelper.PostJson("https://api-fxpractice.oanda.com/v3/accounts/" + this.accountId + "/orders", token, marketOrderJson);

            logger.debug("Response to order: " + orderResponseJson);

            ObjectMapper objectMapper = new ObjectMapper();
            Object jsonObj = objectMapper.readValue(orderResponseJson, Object.class);

            String price = (String)PropertyUtils.getProperty(jsonObj, "orderFillTransaction.price");
            String dateTime = (String)PropertyUtils.getProperty(jsonObj, "orderFillTransaction.time");
            String tradeId = (String)PropertyUtils.getProperty(jsonObj, "orderFillTransaction.tradeOpened.tradeID");

            System.out.println("price" + price);
            System.out.println("tradeID" + tradeId);

            position.setStatus(PositionStatus.OPEN);
            position.setActualOpenPrice((int)(Double.parseDouble(price)*100.0));
            position.setUniqueId(Long.parseLong(tradeId));
            //@TODO: Use dateTime above
            position.setTimeOpened(Calendar.getInstance());
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }

        return true;
    }

    @Override
    public boolean checkPosition(Position position) {
        return false;
    }

    @Override
    public boolean closePosition(Position position) {
        String closePositionJson = " { \"units\": \"ALL\"}";

        logger.debug("Close trade: " + position.getUniqueId() + " json:" + closePositionJson);

        try {
            String closeResponseJson = RestHelper.PutJson("https://api-fxpractice.oanda.com/v3/accounts/" + this.accountId + "/trades/" + position.getUniqueId() + "/close", token, closePositionJson);

            logger.debug("Close response: " + closePositionJson);
            position.setStatus(PositionStatus.CLOSED);
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            ex.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public void getPositions(PositionsManager manager, Collection<ISellConditionProvider> templateSellConditions) {
        Trades trades = null;
        try {
            String jsonResult = RestHelper.GetJson("https://api-fxpractice.oanda.com/v3/accounts/" + this.accountId + "/openTrades", token);
            ObjectMapper jsonDeseserialiser = new ObjectMapper();

            trades = jsonDeseserialiser.readValue(jsonResult, Trades.class);

            for (int pIndex = 0; pIndex < trades.trades.size(); pIndex++) {
                Trade trade = trades.trades.get(pIndex);

                Position internalPosition = new Position();
                internalPosition.setQuantity(trade.currentUnits);
                internalPosition.setActualOpenPrice((int)(trade.price*100));
                internalPosition.setUniqueId(Long.parseLong(trade.id));
                internalPosition.setTimeOpened(javax.xml.bind.DatatypeConverter.parseDateTime(trade.openTime));
                internalPosition.setStatus(PositionStatus.OPEN);

                for (ISellConditionProvider sellPosition : templateSellConditions) {
                    ISellConditionProvider copiedSellCondition = sellPosition.makeCopy();
                    copiedSellCondition.setBuyPrice(internalPosition.getActualOpenPrice());
                    internalPosition.addSellCondition(copiedSellCondition);
                }

                manager.addExistingPosition(internalPosition);

                manager.printStats();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void init() throws Exception{
        this.accounts = this.getAccounts();
        if (this.accounts.accounts.size() <= 0) throw new Exception ("No accounts");
        this.accountId = this.accounts.accounts.stream().findFirst().get().id;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getToken() {
        return this.token;
    }

    public String getAccountId() {
        return this.accountId;
    }


    public Prices getCurrentPrice() {

        Prices prices = null;
        try {
            String jsonResult = RestHelper.GetJson("https://api-fxpractice.oanda.com/v3/accounts/" + this.accountId + "/pricing?instruments=BCO_USD", token);
            ObjectMapper jsonDeseserialiser = new ObjectMapper();

            prices = jsonDeseserialiser.readValue(jsonResult, Prices.class);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return prices;
    }

    public void getAllInstruments() {
       //BCO_USD

        String jsonResult = RestHelper.GetJson("https://api-fxpractice.oanda.com/v3/accounts/" + this.accountId + "/instruments", token);
    }

    public Accounts getAccounts(){
        Accounts accounts = null;
        try {
            String accountsJson = RestHelper.GetJson("https://api-fxpractice.oanda.com/v3/accounts", token);

            ObjectMapper jsonDeseserialiser = new ObjectMapper();
            accounts = jsonDeseserialiser.readValue(accountsJson, Accounts.class);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return accounts;
    }
}

class Account {
    public String id;
    public  Collection<String> tags;
}

class Accounts {
    public Collection<Account> accounts;
}

class Instruments {

    public Collection<Instrument> instruments;

}

class Instrument {
    public String displayName;
    public int displayPrecision;

}