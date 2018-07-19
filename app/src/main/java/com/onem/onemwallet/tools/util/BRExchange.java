package com.onem.onemwallet.tools.util;

import android.content.Context;
import android.util.Log;

import com.onem.onemwallet.presenter.entities.CurrencyEntity;
import com.onem.onemwallet.tools.manager.BRSharedPrefs;
import com.onem.onemwallet.tools.sqlite.CurrencyDataSource;
import com.onem.onemwallet.wallet.BRWalletManager;


import java.math.BigDecimal;

import static com.onem.onemwallet.tools.util.BRConstants.CURRENT_UNIT_PHOTONS;
import static com.onem.onemwallet.tools.util.BRConstants.ROUNDING_MODE;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on <mihail@breadwallet.com> 3/23/17.
 * Copyright (c) 2017 breadwallet LLC
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
public class BRExchange {

    private static final String TAG = BRExchange.class.getName();

    public static BigDecimal getMaxAmount(Context context, String iso) {
        final long MAX_BTC = 84000000;
        if (iso.equalsIgnoreCase("MCN"))
            return getBitcoinForSatoshis(context, new BigDecimal(MAX_BTC * 100000000));
        CurrencyEntity ent = CurrencyDataSource.getInstance().getCurrencyByIso(iso);
        if (ent == null) throw new RuntimeException("no currency in DB for: " + iso);
        return new BigDecimal(ent.rate * MAX_BTC);
    }

    // amount in satoshis
    public static BigDecimal getBitcoinForSatoshis(Context app, BigDecimal amount) {
        BigDecimal result = new BigDecimal(0);
        int unit = BRSharedPrefs.getCurrencyUnit(app);
        switch (unit) {
            case CURRENT_UNIT_PHOTONS:
                result = new BigDecimal(String.valueOf(amount)).divide(new BigDecimal("100"), 2, ROUNDING_MODE);
                break;
            case BRConstants.CURRENT_UNIT_LITES:
                result = new BigDecimal(String.valueOf(amount)).divide(new BigDecimal("100000"), 5, ROUNDING_MODE);
                break;
            case BRConstants.CURRENT_UNIT_LITECOINS:
                result = new BigDecimal(String.valueOf(amount)).divide(new BigDecimal("100000000"), 8, ROUNDING_MODE);
                break;
        }
        return result;
    }

    public static BigDecimal getSatoshisForBitcoin(Context app, BigDecimal amount) {
        BigDecimal result = new BigDecimal(0);
        int unit = BRSharedPrefs.getCurrencyUnit(app);
        switch (unit) {
            case CURRENT_UNIT_PHOTONS:
                result = new BigDecimal(String.valueOf(amount)).multiply(new BigDecimal("100"));
                break;
            case BRConstants.CURRENT_UNIT_LITES:
                result = new BigDecimal(String.valueOf(amount)).multiply(new BigDecimal("100000"));
                break;
            case BRConstants.CURRENT_UNIT_LITECOINS:
                result = new BigDecimal(String.valueOf(amount)).multiply(new BigDecimal("100000000"));
                break;
        }
        return result;
    }

    static String getMcoinSymbol(Context app) {
        String currencySymbolString = BRConstants.mcoinLowercase;
        if (app != null) {
            int unit = BRSharedPrefs.getCurrencyUnit(app);
            switch (unit) {
                case CURRENT_UNIT_PHOTONS:
                    currencySymbolString = "m" + BRConstants.mcoinLowercase;
                    break;
                case BRConstants.CURRENT_UNIT_LITES:
                    currencySymbolString = BRConstants.mcoinLowercase;
                    break;
                case BRConstants.CURRENT_UNIT_LITECOINS:
                    currencySymbolString = BRConstants.mcoinUppercase;
                    break;
            }
        }
        return currencySymbolString;
    }

    //get an iso amount from  satoshis
    public static BigDecimal getAmountFromSatoshis(Context app, String iso, BigDecimal amount) {
        // Log.e(TAG, "getAmountFromSatoshis: " + iso + ":" + amount);
        BigDecimal result;
        if (iso.equalsIgnoreCase("MCN")) {
            result = getBitcoinForSatoshis(app, amount);
        } else {
            //multiply by 100 because core function localAmount accepts the smallest amount e.g. cents
            CurrencyEntity ent = CurrencyDataSource.getInstance().getCurrencyByIso(iso);
            if (ent == null) return new BigDecimal(0);
            Log.e(TAG, "getAmountFromSatoshis: rate" + ent.rate + "iso: " + iso);

            BigDecimal rate = new BigDecimal(ent.rate); //.multiply(new BigDecimal(100));


            // round scale to avoid conversion wierdness like 100 MCN = 2.99 USD instead of 3 USD
            rate = rate.setScale(2, BigDecimal.ROUND_HALF_EVEN);

            rate = rate.multiply(new BigDecimal(100));

            Log.e(TAG, "getAmountFromSatoshis: rate" + rate + "iso: " + iso);

            result = new BigDecimal(BRWalletManager.getInstance().localAmount(amount.longValue(),rate.doubleValue()))
                    .divide(new BigDecimal(100), 2, BRConstants.ROUNDING_MODE);
        }
        // Log.e(TAG, "getAmountFromSatoshis: " + iso + ":RESULT:" + result);
        return result;
    }



    //get satoshis from an iso amount
    public static BigDecimal getSatoshisFromAmount(Context app, String iso, BigDecimal amount) {
//        Log.e(TAG, "getSatoshisFromAmount: " + iso + ":" + amount);
        BigDecimal result;
        if (iso.equalsIgnoreCase("MCN")) {
            result = BRExchange.getSatoshisForBitcoin(app, amount);
        } else {
            //multiply by 100 because core function localAmount accepts the smallest amount e.g. cents
            CurrencyEntity ent = CurrencyDataSource.getInstance().getCurrencyByIso(iso);
            if (ent == null) return new BigDecimal(0);
            BigDecimal rate = new BigDecimal(ent.rate).multiply(new BigDecimal(100));
            result = new BigDecimal(BRWalletManager.getInstance().bitcoinAmount(amount.multiply(new BigDecimal(100)).longValue(),rate.doubleValue()));
        }
//        Log.e(TAG, "getSatoshisFromAmount: " + iso + ":RESULT:" + result);
        return result;
    }
}
