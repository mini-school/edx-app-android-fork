/*
 * Copyright (C) 2014 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.edx.mobile.mediation;

import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.initialization.AdapterStatus;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.nativead.MediaView;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdView;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

import org.edx.mobile.R;
import org.edx.mobile.base.BaseAppActivity;

import java.util.Map;

/**
 * A simple {@link android.app.Activity} that displays adds using the sample adapter and sample
 * custom event.
 */
public class Adverts extends BaseAppActivity {

    // The banner ad view.
    private AdView bannerAdView;
    // The native ad view.
    private AdView nativeAdView;
    // The banner container
    private FrameLayout bannerContainerView;
    // The native container
    private FrameLayout nativeContainerView;
    // A loaded interstitial ad.
    private InterstitialAd interstitial;
    // A loaded rewarded ad.
    private RewardedAd rewardedVideo;
    // The ad loader.
    private AdLoader adLoader;

    private Boolean isAdActivated;
    private Boolean isTestMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_banner_ads);

    }
    
    public void initializeAds() {
        isAdActivated = false;
        isTestMode = true;

        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
                Map<String, AdapterStatus> statusMap = initializationStatus.getAdapterStatusMap();
                for (String adapterClass : statusMap.keySet()) {
                    AdapterStatus status = statusMap.get(adapterClass);
                    Log.d("MiniApp", String.format(
                            "Adapter name: %s Description: %s, Latency: %d",adapterClass, status.getDescription(), status.getLatency()));
                    Toast.makeText(Adverts.this, String.format("Adapter name: %s Description: %s, Latency: %d",adapterClass, status.getDescription(), status.getLatency()), Toast.LENGTH_SHORT).show();
                }
                isAdActivated = true;
                Toast.makeText(Adverts.this, "Mobile Ads initialized succesfully from ads.java", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void loadBannerAd() {
        if (isAdActivated) {
            bannerContainerView = findViewById(R.id.banner_ad_container);
            bannerContainerView.post(new Runnable() {
                @Override
                public void run() {
                    showBannerAd();
                }
            });
        }
    }

    public void showBannerAd() {
        // Banner ads.
        if (isAdActivated) {
            bannerAdView = new AdView(this);
            bannerAdView.setAdUnitId(getBannerAdUnitId());
            AdSize adSize = getBannerAdSize();
            bannerAdView.setAdSize(adSize);
            bannerAdView.setAdListener(new AdListener() {
                @Override
                public void onAdLoaded () {
//        adContainerView.removeAllViews();
//        adContainerView.addView(banner);
//        bannerAdUnitCount = 1;
                }
                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    Toast.makeText(Adverts.this,
                            "Failed to load banner: " + loadAdError,
                            Toast.LENGTH_SHORT).show();
//      change unit id
//      loadBannerAd();
                }
                @Override
                public void onAdOpened() {
//                sufficientRule();
                }
                @Override
                public void onAdClicked() {
                }
                @Override
                public void onAdClosed() {
//                if (isSufficient = true) {
//                    adContainerView.removeAllViews();
//                    freedomRule();
//                } else {
//                    insufficientRule();
//                }
                }
            });
            AdRequest adRequest = new AdRequest.Builder().build();
            adRequest.isTestDevice(this);
            bannerAdView.loadAd(adRequest);
        }
    }

    public void loadInterstitialAd() {
        // Interstitial ads.
        if (isAdActivated) {
            InterstitialAd.load(Adverts.this,
                    getInterstitialAdUnitId(),
                    new AdRequest.Builder().build(),
                    new InterstitialAdLoadCallback() {
                        @Override
                        public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                            interstitial = interstitialAd;
                            interstitial.setFullScreenContentCallback(new FullScreenContentCallback() {
                                @Override
                                public void onAdFailedToShowFullScreenContent(@NonNull AdError error) {
                                    Toast.makeText(Adverts.this,
                                            "Failed to show interstitial: " + error,
                                            Toast.LENGTH_SHORT).show();
//                    loadInterstitialButton.setEnabled(true);
                                }

                                @Override
                                public void onAdDismissedFullScreenContent() {
//                    loadInterstitialButton.setEnabled(true);
                                }
                            });
//                showInterstitialButton.setEnabled(true);
                        }

                        @Override
                        public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                            Toast.makeText(Adverts.this,
                                    "Failed to load interstitial: " + loadAdError,
                                    Toast.LENGTH_SHORT).show();
                            interstitial = null;
//                loadInterstitialButton.setEnabled(true);
                        }
                    });
        }
    }

    public void showInterstitialAd() {
//    showInterstitialButton.setEnabled(false);
        if (isAdActivated) {
            if (interstitial != null) {
                interstitial.show(Adverts.this);
            }
        }
    }

    public void loadRewardedVideoAd() {
        //Sample Adapter Rewarded Ad Button.
//        loadRewardedButton.setEnabled(false);
        if (isAdActivated) {
            RewardedAd.load(Adverts.this,
                    getRewardedAdUnitId(),
                    new AdRequest.Builder().build(),
                    new RewardedAdLoadCallback() {
                        @Override
                        public void onAdLoaded(@NonNull RewardedAd ad) {
                            rewardedVideo = ad;
                            rewardedVideo.setFullScreenContentCallback(new FullScreenContentCallback() {
                                @Override
                                public void onAdFailedToShowFullScreenContent(@NonNull AdError error) {
                                    Toast.makeText(Adverts.this,
                                            "Failed to show interstitial: " + error,
                                            Toast.LENGTH_SHORT).show();
//                        loadRewardedButton.setEnabled(true);
                                }

                                @Override
                                public void onAdDismissedFullScreenContent() {
//                        loadRewardedButton.setEnabled(true);
                                }
                            });
//                    showRewardedButton.setEnabled(true);
                        }

                        @Override
                        public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                            Toast.makeText(Adverts.this,
                                    "Failed to load rewarded ad: " + loadAdError,
                                    Toast.LENGTH_SHORT).show();
                            rewardedVideo = null;
//                    loadRewardedButton.setEnabled(true);
                        }
                    });
        }
    }

    public void showRewardedVideoAd() {
//    showRewardedButton.setEnabled(false);
        if (isAdActivated) {
            if (rewardedVideo != null) {
                rewardedVideo.show(Adverts.this, new OnUserEarnedRewardListener() {
                    @Override
                    public void onUserEarnedReward(@NonNull RewardItem rewardItem) {
                        Toast.makeText(Adverts.this,
                                String.format("User earned reward. Type: %s, amount: %d",
                                        rewardItem.getType(), rewardItem.getAmount()),
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
    }

    public void loadNativeAd () {
        // Native ads.
        if (isAdActivated){
            adLoader = new AdLoader.Builder(this, getNativeAdUnitId())
                    .forNativeAd(new NativeAd.OnNativeAdLoadedListener() {
                        @Override
                        public void onNativeAdLoaded(@NonNull NativeAd nativeAd) {
                            FrameLayout nativeContainer = findViewById(R.id.native_ad_container);
                            NativeAdView adView = (NativeAdView) getLayoutInflater()
                                    .inflate(R.layout.native_ad, null);
                            populateNativeAdView(nativeAd, adView);
                            nativeContainer.removeAllViews();
                            nativeContainer.addView(adView);
                        }
                    })
                    .withAdListener(new AdListener() {
                        @Override
                        public void onAdFailedToLoad(@NonNull LoadAdError error) {
                            Toast.makeText(Adverts.this,
                                    "Failed to load native ad: " + error,
                                    Toast.LENGTH_SHORT).show();
                        }
                    }).build();
            adLoader.loadAd(new AdRequest.Builder().build());

        }
    }

    public void showNativeAd() {
        if (isAdActivated) {

        }
    }

    /**
     * Gets the banner ad unit ID.
     */
    private String getBannerAdUnitId() {
        if (isTestMode) {
            return getResources().getString(R.string.test_banner_ad_unit_id);
        } else {
            return getResources().getString(R.string.prod_banner_ad_unit_id);
        }
    }

    /**
     * Gets the interstitial ad unit ID.
     */
    private String getInterstitialAdUnitId() {
        if (isTestMode) {
            return getResources().getString(R.string.test_interstitial_ad_unit_id);
        } else {
            return getResources().getString(R.string.prod_interstitial_ad_unit_id);
        }
    }

    /**
     * Gets the rewarded ad unit ID to test.
     */
    private String getRewardedAdUnitId() {
        if (isTestMode) {
            return getResources().getString(R.string.test_rewarded_ad_unit_id);
        } else {
            return getResources().getString(R.string.prod_rewarded_ad_unit_id);
        }
    }

    /**
     * Gets the native ad unit ID to test.
     */
    private String getNativeAdUnitId() {
        if (isTestMode) {
            return getResources().getString(R.string.test_native_ad_unit_id);
        } else {
            return getResources().getString(R.string.prod_native_ad_unit_id);
        }
    }

    /**
     *  Gets the Adaptive Banner size.
     * @return
     */

    private AdSize getBannerAdSize() {
        // Determine the screen width (less decorations) to use for the ad width.
        Display display = getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        float density = outMetrics.density;

        float adWidthPixels = bannerContainerView.getWidth();

        // If the ad hasn't been laid out, default to the full screen width.
        if (adWidthPixels == 0) {
            adWidthPixels = outMetrics.widthPixels;
        }

        int adWidth = (int) (adWidthPixels / density);
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, adWidth);
    }

    /**
     * Populates a {@link NativeAdView} object with data from a given {@link NativeAd}.
     *
     * @param nativeAd the object containing the ad's assets
     * @param adView the view to be populated
     */
    private void populateNativeAdView(NativeAd nativeAd, NativeAdView adView) {
        // Set the media view. Media content will be automatically populated in the media view once
        // adView.setNativeAd() is called.
        MediaView mediaView = adView.findViewById(R.id.ad_media);
        adView.setMediaView(mediaView);

        // Set other ad assets.
        adView.setHeadlineView(adView.findViewById(R.id.ad_headline));
        adView.setBodyView(adView.findViewById(R.id.ad_body));
        adView.setCallToActionView(adView.findViewById(R.id.ad_call_to_action));
        adView.setIconView(adView.findViewById(R.id.ad_app_icon));
        adView.setPriceView(adView.findViewById(R.id.ad_price));
        adView.setStarRatingView(adView.findViewById(R.id.ad_stars));
        adView.setStoreView(adView.findViewById(R.id.ad_store));
        adView.setAdvertiserView(adView.findViewById(R.id.ad_advertiser));

        // The headline is guaranteed to be in every NativeAd.
        ((TextView) adView.getHeadlineView()).setText(nativeAd.getHeadline());

        // These assets aren't guaranteed to be in every NativeAd, so it's important to
        // check before trying to display them.
        if (nativeAd.getBody() == null) {
            adView.getBodyView().setVisibility(View.INVISIBLE);
        } else {
            adView.getBodyView().setVisibility(View.VISIBLE);
            ((TextView) adView.getBodyView()).setText(nativeAd.getBody());
        }

        if (nativeAd.getCallToAction() == null) {
            adView.getCallToActionView().setVisibility(View.INVISIBLE);
        } else {
            adView.getCallToActionView().setVisibility(View.VISIBLE);
            ((Button) adView.getCallToActionView()).setText(nativeAd.getCallToAction());
        }

        if (nativeAd.getIcon() == null) {
            adView.getIconView().setVisibility(View.GONE);
        } else {
            ((ImageView) adView.getIconView()).setImageDrawable(
                    nativeAd.getIcon().getDrawable());
            adView.getIconView().setVisibility(View.VISIBLE);
        }

        if (nativeAd.getPrice() == null) {
            adView.getPriceView().setVisibility(View.INVISIBLE);
        } else {
            adView.getPriceView().setVisibility(View.VISIBLE);
            ((TextView) adView.getPriceView()).setText(nativeAd.getPrice());
        }

        if (nativeAd.getStore() == null) {
            adView.getStoreView().setVisibility(View.INVISIBLE);
        } else {
            adView.getStoreView().setVisibility(View.VISIBLE);
            ((TextView) adView.getStoreView()).setText(nativeAd.getStore());
        }

        if (nativeAd.getStarRating() == null || nativeAd.getStarRating() < 3) {
            adView.getStarRatingView().setVisibility(View.INVISIBLE);
        } else {
            ((RatingBar) adView.getStarRatingView())
                    .setRating(nativeAd.getStarRating().floatValue());
            adView.getStarRatingView().setVisibility(View.VISIBLE);
        }

        if (nativeAd.getAdvertiser() == null) {
            adView.getAdvertiserView().setVisibility(View.INVISIBLE);
        } else {
            ((TextView) adView.getAdvertiserView()).setText(nativeAd.getAdvertiser());
            adView.getAdvertiserView().setVisibility(View.VISIBLE);
        }

        // This method tells the Google Mobile Ads SDK that you have finished populating your
        // native ad view with this native ad. The SDK will populate the adView's MediaView
        // with the media content from this native ad.
        adView.setNativeAd(nativeAd);
    }
}
