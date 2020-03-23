import re


def load_data(df, split=0.8):
    texts = df['text'].tolist()
    labels = df['sentiment'].tolist()
    cats = [{"POSITIVE": bool(y), "NEGATIVE": not bool(y)} for y in labels]
    split = int(len(texts) * split)
    return (texts[:split], cats[:split]), (texts[split:], cats[split:])


def clean_data(df):
    df['text'] = df['text'].map(clean_tweet)


def clean_tweet(tweet):
    tweet = re.sub(r"@[A-Za-z0-9_]+", "", tweet)  # mentions
    tweet = re.sub(r"https?://[A-Za-z0-9./]+", "", tweet)  # URLs
    tweet = re.sub(r"[:=;][oO\-]?[D\)\]\(\]/\\OpP]", "", tweet) # emoticons
    tweet = tweet.replace('#', '')
    return tweet.strip()
