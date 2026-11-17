package com.example.data

import com.example.model.OfferCategory
import com.example.model.OfferModel

object OfferCatalogSeed {
    fun curatedOffers(): List<OfferModel> = listOf(
        OfferModel(
            title = "H > 150 জিবি 30 দিন 898৳ অল বিডি সবাই পাবে",
            price = "898",
            buyPrice = "748",
            discount = "discount: ৳150",
            id = "drive_150gb_898",
            category = OfferCategory.INTERNET,
            badge = "All BD",
            rating = 4.9f,
            description = "150 GB high-speed 4G drive internet pack valid for 30 days across all divisions of Bangladesh.",
            isFeatured = true
        ),
        OfferModel(
            title = "H > আনলিমিটেড ইন্টারনেট 10 এমবি পিএস 30দিন_806",
            price = "806",
            buyPrice = "706",
            discount = "discount: ৳100",
            id = "drive_unlimited_10mbps_806",
            category = OfferCategory.UNLIMITED,
            badge = "10 Mbps",
            rating = 4.8f,
            description = "Unlimited 10 Mbps drive internet plan for 30 days with direct instant activation."
        ),
        OfferModel(
            title = "H > 30 জিবি 700 মিনিট 30দিন_818৳ (শুধু বরিশাল বিভাগে)",
            price = "818",
            buyPrice = "668",
            discount = "discount: ৳150",
            id = "drive_30gb_700min_barishal_818",
            category = OfferCategory.REGIONAL,
            badge = "Barishal",
            rating = 4.9f,
            description = "30 GB internet + 700 minutes combo bundle valid for 30 days exclusively in Barishal division."
        ),
        OfferModel(
            title = "H > 50 জিবি 1000 মিনিট 30 দিন 998৳ অল বিডি",
            price = "998",
            buyPrice = "828",
            discount = "discount: ৳170",
            id = "drive_50gb_1000min_998",
            category = OfferCategory.COMBO,
            badge = "Combo Best",
            rating = 4.8f,
            description = "50 GB 4G data plus 1000 all-operator minutes valid for 30 days nationwide."
        ),
        OfferModel(
            title = "H > 1000 মিনিট 30 দিন 648৳ অল বিডি",
            price = "648",
            buyPrice = "548",
            discount = "discount: ৳100",
            id = "drive_1000min_648",
            category = OfferCategory.MINUTES,
            badge = "Talktime",
            rating = 4.7f,
            description = "1000 anytime voice minutes valid for 30 days across all local networks."
        ),
        OfferModel(
            title = "H > 80 জিবি 30 দিন 698৳ ঢাকা ও চট্টগ্রাম বিভাগ",
            price = "698",
            buyPrice = "578",
            discount = "discount: ৳120",
            id = "drive_80gb_dhaka_ctg_698",
            category = OfferCategory.REGIONAL,
            badge = "Dhaka & Ctg",
            rating = 4.8f,
            description = "80 GB regional drive pack for 30 days in Dhaka and Chattogram divisions."
        )
    )
}
