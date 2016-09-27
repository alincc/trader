package com.futurewebdynamics.trader.positions;

import com.futurewebdynamics.trader.common.NormalisedPriceInformation;
import com.futurewebdynamics.trader.riskfilters.IRiskFilter;
import com.futurewebdynamics.trader.sellconditions.ISellConditionProvider;
import com.futurewebdynamics.trader.trader.ITrader;
import org.apache.log4j.Logger;

import java.util.*;

/**
 * Created by 52con on 15/04/2016.
 */
public class PositionsManager {

    public List<Position> positions;
    public List<IRiskFilter> riskFilters;

    private ITrader trader;

    final static Logger logger = Logger.getLogger(PositionsManager.class);

    public PositionsManager() {

        positions = Collections.synchronizedList(new ArrayList<Position>());
    }

    public ITrader getTrader() {
        return trader;
    }

    public void setTrader(ITrader trader) {
        this.trader = trader;
    }

    public void init() {

    }

    public void tick(NormalisedPriceInformation tickData) {
        logger.debug("Calling tick() on " + this.positions.size() + " positions");
        if (tickData.isEmpty()) {
            logger.debug("tick data is empty");
            return;
        }

        for (int i = 0; i < this.positions.size(); i++)
        {
            Position position = this.positions.get(i);
            if (position.getStatus() == PositionStatus.OPEN) {
                position.tick(tickData);
            }
        }
    }

    public void openPosition(int price, Collection<ISellConditionProvider> templateSellConditions) {

        for (IRiskFilter riskFilter : this.riskFilters) {
            if (!riskFilter.proceedWithBuy(price)) {
                return;
            }
        }

        Position position = new Position();
        position.setPositionsManager(this);

        this.positions.add(position);
        position.setStatus(PositionStatus.BUYING);
        position.setTargetOpenPrice(price);

        Calendar cal = GregorianCalendar.getInstance();
        position.setTimeOpened(cal);
        position.setActualOpenPrice(price);
        position.setTargetOpenPrice(price);


        for (ISellConditionProvider sellPosition : templateSellConditions) {
            ISellConditionProvider copiedSellCondition = sellPosition.makeCopy();
            copiedSellCondition.setBuyPrice(price);
            position.addSellCondition(copiedSellCondition);
        }

        this.trader.openPosition(position);
        logger.debug("Position Status: " + position.getStatus().toString());

    }

    public void sellPosition(Position position, int targetPrice) {
        logger.info("Selling position " + position.getUniqueId() + " opened at " + position.getActualOpenPrice() + " for " + targetPrice);
        Calendar cal = GregorianCalendar.getInstance();
        position.setTimeClosed(cal);

        position.setStatus(PositionStatus.SELLING);
        position.setTargetSellPrice(targetPrice);
        position.setActualSellPrice(targetPrice);
        this.trader.closePosition(position);
    }

    public void printStats() {

        logger.debug(String.format("%-7s%-14s%-7s%-7s%-14s%-7s%-7s%-7s\n", new String[]{"ID", "TO", "TOP", "AOP", "TC", "TCP", "ACP", "Q"}));
        System.out.format("%-7s%-14s%-7s%-7s%-14s%-7s%-7s%-7s\n", new String[]{"ID", "TO", "TOP", "AOP", "TC", "TCP", "ACP", "Q"});

        Iterator izzy = positions.iterator();
        while(izzy.hasNext()) {
            Position position = (Position)izzy.next();
            logger.debug(String.format("%-7d%-14d%-7d%-7d%-14d%-7d%-7d%-7d\n",position.getUniqueId(), position.getTimeOpened().getTimeInMillis()/1000, position.getTargetOpenPrice(), position.getActualOpenPrice(), (position.getTimeClosed() !=null) ? position.getTimeClosed().getTimeInMillis()/1000 : 0, position.getTargetSellPrice(), position.getActualSellPrice(), position.getQuantity()));
            System.out.format("%-7d%-14d%-7d%-7d%-14d%-7d%-7d%-7d\n", position.getUniqueId(), position.getTimeOpened().getTimeInMillis()/1000, position.getTargetOpenPrice(), position.getActualOpenPrice(), (position.getTimeClosed() !=null) ? position.getTimeClosed().getTimeInMillis()/1000 : 0, position.getTargetSellPrice(), position.getActualSellPrice(), position.getQuantity());

        }

        int totalTrades = (int)positions.stream().filter(p -> p.getStatus() == PositionStatus.CLOSED).count();

        logger.debug("Total closed trades: " + totalTrades);
        System.out.println("Total closed trades: " + totalTrades);
        logger.debug("% closed at profit: " + positions.stream().filter(p->p.getStatus()==PositionStatus.CLOSED && p.getActualSellPrice() > p.getActualOpenPrice()).count() / (double)totalTrades * 100);
        System.out.println("% closed at profit: " + positions.stream().filter(p->p.getStatus()==PositionStatus.CLOSED && p.getActualSellPrice() > p.getActualOpenPrice()).count() / (double)totalTrades * 100);

        logger.debug("Average Profit: " + positions.stream().filter(p->p.getStatus()==PositionStatus.CLOSED && p.getActualSellPrice() > p.getActualOpenPrice()).mapToInt(p->p.getActualSellPrice() - p.getActualOpenPrice()).average());
        System.out.println("Average Profit: " + positions.stream().filter(p->p.getStatus()==PositionStatus.CLOSED && p.getActualSellPrice() > p.getActualOpenPrice()).mapToInt(p->p.getActualSellPrice() - p.getActualOpenPrice()).average());
        logger.debug("Average Loss: " + positions.stream().filter(p->p.getStatus()==PositionStatus.CLOSED && p.getActualSellPrice() < p.getActualOpenPrice()).mapToInt(p->p.getActualOpenPrice() - p.getActualSellPrice()).average());
        System.out.println("Average Loss: " + positions.stream().filter(p->p.getStatus()==PositionStatus.CLOSED && p.getActualSellPrice() < p.getActualOpenPrice()).mapToInt(p->p.getActualOpenPrice() - p.getActualSellPrice()).average());

        logger.debug("Total Gains: " + positions.stream().filter(p -> p.getStatus() == PositionStatus.CLOSED).mapToInt(p->p.getActualSellPrice() - p.getActualOpenPrice()).sum());
        System.out.println("Total Gains: " + positions.stream().filter(p -> p.getStatus() == PositionStatus.CLOSED).mapToInt(p->p.getActualSellPrice() - p.getActualOpenPrice()).sum());
    }

    public int getTotalGains() {
        return positions.stream().filter(p -> p.getStatus() == PositionStatus.CLOSED).mapToInt(p->p.getActualSellPrice() - p.getActualOpenPrice()).sum();
    }

    public void addExistingPosition(Position p) {
        p.setPositionsManager(this);
        this.positions.add(p);
    }
}
