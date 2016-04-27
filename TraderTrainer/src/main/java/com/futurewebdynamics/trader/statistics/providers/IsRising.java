package com.futurewebdynamics.trader.statistics.providers;

import com.futurewebdynamics.trader.common.DataWindow;
import com.futurewebdynamics.trader.statistics.IStatisticProvider;
import com.futurewebdynamics.trader.common.NormalisedPriceInformation;

import java.util.List;

/**
 * Created by 52con on 15/04/2016.
 */
public class IsRising extends IStatisticProvider {

    private int lookBack;

    public IsRising(int lookBack) {
        this.lookBack = lookBack;
    }

    public int getLookBack() {
        return lookBack;
    }

    public void setLookBack(int lookBack) {
        this.lookBack = lookBack;
    }

    @Override
    public int getMajorVersion() {
        return 0;
    }

    @Override
    public int getMinorVersion() {
        return 1;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public DataWindow getDataWindow() {
        return super.getDataWindow();
    }

    @Override
    public void setDataWindow(DataWindow dataWindow) {
        super.setDataWindow(dataWindow);
    }

    @Override
    public Object getResult() {
        dataWindow.debug();

        List<NormalisedPriceInformation> data = dataWindow.getData();

        int lastValue = data.get(0).getPrice();

        for (int i = 1; i <= lookBack; i++) {
            int testValue = data.get(lookBack).getPrice();
            if (testValue >= lastValue) return false;
            lastValue = testValue;
        }

        return true;
    }
}
