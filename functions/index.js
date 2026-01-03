const { onCall } = require("firebase-functions/v2/https");
const { defineSecret, defineString } = require("firebase-functions/params");
const logger = require("firebase-functions/logger");
const axios = require("axios");

const TELEGRAM_TOKEN = defineSecret("TELEGRAM_TOKEN");
const TELEGRAM_CHAT_ID = defineString("TELEGRAM_CHAT_ID");

exports.sendOrderToTelegram = onCall(
  {
    secrets: [TELEGRAM_TOKEN],
    cors: true,
  },
  async (request) => {
    try {
      const order = request.data;

      const token = TELEGRAM_TOKEN.value();
      const chatId = TELEGRAM_CHAT_ID.value();

      if (!token) throw new Error("Нет TELEGRAM_TOKEN");
      if (!chatId) throw new Error("Нет TELEGRAM_CHAT_ID");

      // ---------- СООБЩЕНИЕ ----------
      let text = `🧾 НОВЫЙ ЗАКАЗ\n\n`;

      // ----- ТОВАРЫ -----
      text += `🛒 Товары:\n`;

      order.items.forEach((item, index) => {
        const qty = item.quantity;
        const unit = item.unit === "KG" ? "кг" : "шт";
        const price = item.price;
        const sum = qty * price;

        text += `${index + 1}) ${item.name} — ${qty} ${unit} × ${price} ₽ = ${sum} ₽\n`;
      });

      // ----- ИТОГ -----
      text += `\n💰 Итого: ~ ${order.total} ₽\n`;
      text += `(Фактическая сумма может немного отличаться из-за точного веса)\n\n`;

      // ----- КЛИЕНТ -----
      text += `👤 Имя: ${order.customerName}\n`;
      text += `📞 Телефон: ${order.customerPhone}\n`;
      text += `📍 Адрес: ${order.customerAddress}\n`;

      if (order.comment && order.comment.trim() !== "") {
        text += `📝 Комментарий: ${order.comment}\n`;
      }

      // ---------- ОТПРАВКА ----------
      const url = `https://api.telegram.org/bot${token}/sendMessage`;

      await axios.post(url, {
        chat_id: chatId,
        text: text,
      });

      logger.info("Заказ отправлен в Telegram");
      return { ok: true };

    } catch (e) {
      logger.error("Ошибка Telegram", e);
      throw new Error(e.message);
    }
  }
);
