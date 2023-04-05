package com.idevhub.coino.tradingengine.service.settings;

import com.idevhub.coino.settings.entity.SettingItem;
import com.idevhub.coino.settings.service.SettingsManager;
import com.idevhub.coino.tradingmodel.entity.enumeration.MarketStatus;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class TradingEngineSettingsManager {
    private final SettingsManager settingsManager;

    public boolean isFrozenMarketStatus() {
        SettingItem settingItem = getSettingItem(TradingEngineSettings.MARKET_STATUS.name());
        val marketStatus = settingsManager.getAs(settingItem, MarketStatus.class);

        return marketStatus == MarketStatus.FROZEN;
    }

    public void setMarketStatus(MarketStatus marketStatus) {
        SettingItem settingItem = getSettingItem(TradingEngineSettings.MARKET_STATUS.name());
        settingsManager.set(settingItem, marketStatus);
    }

    private SettingItem getSettingItem(String itemName) {
        try {
            Optional<Enum> setting = Stream.of(TradingEngineSettings.class.getEnumConstants())
                .map(e -> (Enum) e)
                .filter(e -> e.name().equals(itemName))
                .findFirst();

            return (SettingItem) setting.get();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public BigDecimal getMinAmount() {
        SettingItem settingItem = getSettingItem(TradingEngineSettings.MIN_AMOUNT.name());
        return settingsManager.getAs(settingItem, BigDecimal.class);
    }
}
