package com.bohdankukuruza.scamdetector.data.samples

/**
 * Curated sample messages shown as quick-fill chips in the UI.
 *
 * Each sample exercises one or more detection rules so the user can
 * see the analyzer respond to realistic scam patterns without having
 * to type out a full message. Wording is paraphrased from publicly
 * reported scam campaigns (FTC, Norton scam database, An Post fraud
 * alerts).
 */
data class SampleMessage(
    val label: String,
    val text: String
)

object SampleMessages {

    val all: List<SampleMessage> = listOf(
        SampleMessage(
            label = "Fake delivery",
            text = "An Post: Your package could not be delivered due to unpaid customs fee €2.99. " +
                    "Please update your details urgently at https://anpost-redelivery.top/track to avoid return."
        ),
        SampleMessage(
            label = "Bank phishing",
            text = "URGENT: Unusual activity detected on your AIB account. " +
                    "Verify your identity immediately at bit.ly/aib-secure-login or your account will be locked."
        ),
        SampleMessage(
            label = "Prize scam",
            text = "CONGRATULATIONS!!! YOU HAVE WON A €1000 AMAZON GIFT CARD. " +
                    "CLAIM YOUR PRIZE NOW BEFORE IT EXPIRES TODAY: tinyurl.com/free-amzn-prize"
        ),
        SampleMessage(
            label = "Legit message",
            text = "Hey, just confirming dinner at 7pm tomorrow. Let me know if anything changes!"
        )
    )
}