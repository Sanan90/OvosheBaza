const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { defineSecret, defineString } = require("firebase-functions/params");
const logger = require("firebase-functions/logger");
const axios = require("axios");

// Секрет (токен бота) — хранится в Secret Manager
const TELEGRAM_TOKEN = defineSecret("TELEGRAM_TOKEN");

// chat_id группы/чата — строковый параметр
const TELEGRAM_CHAT_ID = defineString("TELEGRAM_CHAT_ID");

// ---------- helpers ----------
function isNonEmptyString(v) {
  return typeof v === "string" && v.trim().length > 0;
}

function num(v, def = 0) {
  const n = Number(v);
  return Number.isFinite(n) ? n : def;
}

function formatQty(qty, unit) {
  // qty может быть 0.5, 1, 2 и т.д.
  // Для кг показываем до 3 знаков, но без лишних нулей
  if (unit === "KG") {
    const s = qty.toFixed(3).replace(/0+$/, "").replace(/\.$/, "");
    return s.length ? s : "0";
  }
  // Для шт — целое
  return String(Math.round(qty));
}

function unitLabel(unit) {
  return unit === "KG" ? "кг" : "шт";
}

// ---------- main function ----------
exports.sendOrderToTelegram = onCall(
  {
    secrets: [TELEGRAM_TOKEN],
    cors: true,
  },
  async (request) => {
    try {
      const data = request.data || {};

      const token = TELEGRAM_TOKEN.value();
      const chatId = TELEGRAM_CHAT_ID.value();

      if (!token) throw new HttpsError("failed-precondition", "Нет TELEGRAM_TOKEN");
      if (!chatId) throw new HttpsError("failed-precondition", "Нет TELEGRAM_CHAT_ID");

      const type = String(data.type || "ORDER").toUpperCase();

      // ---- ЛОГИ (безопасно, токен не печатаем) ----
      logger.info("sendOrderToTelegram called", {
        type,
        tokenLen: token ? token.length : 0,
        tokenPrefix: token ? token.slice(0, 6) : null,
        chatId,
      });

      let text = "";

      // =========================================================
      // SUPPORT
      // =========================================================
      if (type === "SUPPORT") {
        const phone = String(data.phone || "").trim();
        const question = String(data.question || "").trim();

        if (!isNonEmptyString(phone)) {
          throw new HttpsError("invalid-argument", "В поддержке не передан phone");
        }
        if (!isNonEmptyString(question)) {
          throw new HttpsError("invalid-argument", "В поддержке не передан question");
        }

        text =
          `🆘 ОБРАЩЕНИЕ В ПОДДЕРЖКУ\n\n` +
          `📞 Телефон: ${phone}\n\n` +
          `❓ Вопрос:\n${question}\n`;

      } else {
        // =========================================================
        // ORDER (по умолчанию)
        // =========================================================

        const customerName = String(data.customerName || "-");
        const customerPhone = String(data.customerPhone || "-");
        const customerAddress = String(data.customerAddress || "-");
        const comment = String(data.comment || "").trim();

        const items = Array.isArray(data.items) ? data.items : [];

        // Если это заказ, но items пустой — лучше явно сказать
        if (items.length === 0) {
          throw new HttpsError("invalid-argument", "В заказе нет items (пусто или не массив)");
        }

        let itemsText = "";
        let calcTotal = 0;

        items.forEach((item, index) => {
          const name = String(item.name || "Без названия");
          const unit = String(item.unit || "KG").toUpperCase(); // KG / PIECE
          const qty = num(item.quantity, 0);
          const price = num(item.price, 0);

          // sum может прийти готовым, но если нет — считаем сами
          const sum = num(item.sum, qty * price);

          calcTotal += sum;

          itemsText +=
            `${index + 1}) ${name} — ` +
            `${formatQty(qty, unit)} ${unitLabel(unit)} × ${Math.round(price)} ₽ = ${Math.round(sum)} ₽\n`;
        });

        const total = num(data.total, calcTotal);

        text =
          `🧾 НОВЫЙ ЗАКАЗ\n\n` +
          `👤 Имя: ${customerName}\n` +
          `📞 Телефон: ${customerPhone}\n` +
          `📍 Адрес: ${customerAddress}\n` +
          (comment ? `📝 Комментарий: ${comment}\n` : "") +
          `\n🛒 Товары:\n${itemsText}` +
          `\n💰 Итого: ~ ${Math.round(total)} ₽\n` +
          `(Фактическая сумма может немного отличаться из-за точного веса)\n`;
      }

      // ---- отправка в Telegram ----
      const url = `https://api.telegram.org/bot${token}/sendMessage`;

      const tgResp = await axios.post(url, {
        chat_id: chatId,
        text,
      });

      if (!tgResp.data || tgResp.data.ok !== true) {
        logger.error("Telegram API error", tgResp.data);
        throw new HttpsError("internal", "Telegram API error", tgResp.data);
      }

      logger.info("Message sent to Telegram", { type });
      return { ok: true, type };

    } catch (e) {
      logger.error("sendOrderToTelegram error", e);

      // Если это уже HttpsError — пробрасываем как есть
      if (e instanceof HttpsError) throw e;

      // Иначе превращаем в internal
      throw new HttpsError("internal", e?.message ? String(e.message) : "Unknown error");
    }
  }
);
