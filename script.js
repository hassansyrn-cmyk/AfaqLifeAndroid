let isAed = true;
let isAr = true;
let goldData = null;
const aedRate = 3.6725;

const translations = {
    ar: {
        title: "سوق الذهب في الإمارات",
        toggleCurrency: "تغيير العملة (درهم/دولار)",
        toggleLang: "English",
        lastUpdated: "آخر تحديث: ",
        loading: "جاري التحميل...",
        aed: "درهم",
        usd: "دولار"
    },
    en: {
        title: "UAE Gold Market",
        toggleCurrency: "Switch Currency (AED/USD)",
        toggleLang: "العربية",
        lastUpdated: "Last Updated: ",
        loading: "Loading...",
        aed: "AED",
        usd: "USD"
    }
};

async function fetchPrices() {
    try {
        const response = await fetch('https://api.gold-api.com/price/XAU');
        const data = await response.json();
        const ounceToGram = 31.1034768;
        const price24k = data.price / ounceToGram;

        goldData = {
            p24: price24k,
            p22: price24k * (22/24),
            p21: price24k * (21/24),
            p18: price24k * (18/24),
            updatedAt: new Date(data.updatedAt).toLocaleString(isAr ? 'ar-EG' : 'en-US')
        };

        document.getElementById('loading').classList.add('hidden');
        document.getElementById('main-content').classList.remove('hidden');
        render();
    } catch (error) {
        document.getElementById('loading').innerText = "خطأ في تحميل البيانات / Error loading data";
    }
}

function render() {
    const lang = isAr ? 'ar' : 'en';
    const currency = isAed ? translations[lang].aed : translations[lang].usd;
    const multiplier = isAed ? aedRate : 1;

    document.getElementById('title').innerText = translations[lang].title;
    document.getElementById('toggleCurrency').innerText = translations[lang].toggleCurrency;
    document.getElementById('toggleLang').innerText = translations[lang].toggleLang;
    document.getElementById('updatedAt').innerText = translations[lang].lastUpdated + goldData.updatedAt;

    const keys = ['p24', 'p22', 'p21', 'p18'];
    keys.forEach(key => {
        document.getElementById(key).innerText = `${(goldData[key] * multiplier).toFixed(2)} ${currency}`;
    });

    document.querySelectorAll('.label').forEach(el => {
        el.innerText = isAr ? el.getAttribute('data-ar') : el.getAttribute('data-en');
    });

    document.body.dir = isAr ? 'rtl' : 'ltr';
}

document.getElementById('toggleCurrency').onclick = () => {
    isAed = !isAed;
    render();
};

document.getElementById('toggleLang').onclick = () => {
    isAr = !isAr;
    render();
};

fetchPrices();
setInterval(fetchPrices, 60000); // Update every minute
