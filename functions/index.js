const { onCall } = require("firebase-functions/v2/https");
const logger = require("firebase-functions/logger");
const axios = require("axios");
const { defineSecret, defineString } = require("firebase-functions/params");

const TELEGRAM_TOKEN = defineSecret("TELEGRAM_TOKEN");
const TELEGRAM_CHAT_ID = defineString("TELEGRAM_CHAT_ID");

// Красивое число: убираем 0.30000000004 и лишние нули
function fmtNum(n, digits = 3) {
  const x = Number(n);
  if (!Number.isFinite(x)) return "-";
  const fixed = x.toFixed(digits);
  return fixed.replace(/\.?0+$/, ""); // "1.000" -> "1", "0.500"->"0.5"
}

function fmtMoney(n) {
  const x = Number(n);
  if (!Number.isFinite(x)) return "-";
  // можно без копеек:
  return Math.round(x).toString();
}

exports.sendOrderToTelegram = onCall(
  {
    cors: true,
    secrets: [TELEGRAM_TOKEN],
  },
  async (request) => {
    try {
      const data = request.data || {};
      const type = String(data.type || "ORDER").toUpperCase();

      const token = TELEGRAM_TOKEN.value();
      const chatId = TELEGRAM_CHAT_ID.value();

      if (!token) throw new Error("TELEGRAM_TOKEN пустой");
      if (!chatId) throw new Error("TELEGRAM_CHAT_ID пустой");

      let text = "";

      // =========================
      // 1) SUPPORT (поддержка)
      // =========================
      if (type === "SUPPORT") {
        const phone = (data.phone || "").toString().trim();
        const question = (data.question || "").toString().trim();

        if (!question) throw new Error("В поддержке нет question");

        text =
          `🆘 ОБРАЩЕНИЕ В ПОДДЕРЖКУ\n\n` +
          `📞 Телефон: ${phone || "-"}\n\n` +
          `💬 Вопрос:\n${question}\n`;

      // =========================
      // 2) REQUEST (заявка на товар)
      // =========================
      } else if (type === "REQUEST") {
        const customerName = (data.customerName || "").toString().trim();
        const customerPhone = (data.customerPhone || "").toString().trim();
        const requestedProduct = (data.requestedProduct || "").toString().trim();
        const requestedQuantity = (data.requestedQuantity || "").toString().trim();
        const comment = (data.comment || "").toString().trim();

        if (!requestedProduct) throw new Error("В заявке нет requestedProduct");
        if (!customerPhone && !customerName) throw new Error("В заявке нет контактов (имя/телефон)");

        text =
          `📝 ЗАЯВКА НА ТОВАР\n\n` +
          `👤 Имя: ${customerName || "-"}\n` +
          `📞 Телефон: ${customerPhone || "-"}\n\n` +
          `🛒 Что нужно заказать:\n${requestedProduct}\n\n` +
          `⚖️ Количество:\n${requestedQuantity || "-"}\n` +
          (comment ? `\n💬 Комментарий:\n${comment}\n` : "");

      // =========================
      // 3) ORDER (заказ из корзины)
      // =========================
      } else {
        const items = data.items;

        if (!Array.isArray(items) || items.length === 0) {
          throw new Error("В заказе нет items (пусто или не массив)");
        }

        const customerName = (data.customerName || "").toString().trim();
        const customerPhone = (data.customerPhone || "").toString().trim();
        const customerAddress = (data.customerAddress || "").toString().trim();
        const comment = (data.comment || "").toString().trim();

        // Красивый список как чек
        const itemsText = items
          .map((it) => {
            const name = (it.name || "").toString();
            const qty = it.quantity;
            const unit = (it.unit || "").toString().toUpperCase(); // "KG" или "PIECE"
            const price = it.price;
            const sum = it.sum;

            const unitLabel = unit === "KG" ? "кг" : "шт";

            return `• ${name} — ${fmtNum(qty)} ${unitLabel} × ${fmtMoney(price)} = ${fmtMoney(sum)}`;
          })
          .join("\n");

        const total = fmtMoney(data.total);
        const subtotal = fmtMoney(data.subtotal);
        const deliveryFee = fmtMoney(data.deliveryFee);
        const discount = fmtMoney(data.discount);

        text =
          `🧾 НОВЫЙ ЗАКАЗ\n\n` +
          `👤 Имя: ${customerName || "-"}\n` +
          `📞 Телефон: ${customerPhone || "-"}\n` +
          `📍 Адрес: ${customerAddress || "-"}\n` +
          (comment ? `💬 Комментарий: ${comment}\n` : "") +
          `\n🛒 Товары:\n${itemsText}\n\n` +
          `💵 Подитог: ${subtotal}\n` +
          `🚚 Доставка: ${deliveryFee}\n` +
          `🏷 Скидка: ${discount}\n` +
          `💰 ИТОГО: ${total}`;
      }

      const url = `https://api.telegram.org/bot${token}/sendMessage`;

      const tgResp = await axios.post(url, {
        chat_id: chatId,
        text,
      });

      logger.info("Telegram sent ok", tgResp.data);
      return { ok: true };
    } catch (e) {
      logger.error("sendOrderToTelegram error", e);
      throw new Error(e.message || String(e));
    }
  }
);
