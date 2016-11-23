package com.futurewebdynamics.trader.datasources;

import com.futurewebdynamics.trader.common.NormalisedPriceInformation;

/**
 * Created by 52con on 15/04/2016.
 */
public interface IDataSource {

    NormalisedPriceInformation getTickData() throws Exception;

    void init(String propertiesFile) throws Exception;

}
