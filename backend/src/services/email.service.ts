import nodemailer from 'nodemailer'

const transporter = nodemailer.createTransport({
  host: process.env.SMTP_HOST || 'smtp.163.com',
  port: parseInt(process.env.SMTP_PORT || '465'),
  secure: process.env.SMTP_SECURE === 'true',
  auth: {
    user: process.env.SMTP_USER,
    pass: process.env.SMTP_PASS
  }
})

export interface SendMailOptions {
  to: string
  subject: string
  html: string
  text?: string
}

export const sendMail = async (options: SendMailOptions): Promise<boolean> => {
  try {
    const info = await transporter.sendMail({
      from: `"Easy1Auth" <${process.env.SMTP_USER}>`,
      to: options.to,
      subject: options.subject,
      html: options.html,
      text: options.text
    })
    
    console.log('邮件发送成功:', info.messageId)
    return true
  } catch (error) {
    console.error('邮件发送失败:', error)
    return false
  }
}

export const sendVerificationCode = async (email: string, code: string): Promise<boolean> => {
  const html = `
    <!DOCTYPE html>
    <html>
    <head>
      <meta charset="utf-8">
      <style>
        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; line-height: 1.6; color: #333; }
        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
        .header { text-align: center; padding: 20px 0; border-bottom: 1px solid #eee; }
        .logo { font-size: 24px; font-weight: bold; color: #0369A1; }
        .content { padding: 30px 0; }
        .code-box { background: #f5f7fa; border-radius: 8px; padding: 20px; text-align: center; margin: 20px 0; }
        .code { font-size: 32px; font-weight: bold; letter-spacing: 8px; color: #0369A1; }
        .notice { font-size: 14px; color: #666; margin-top: 20px; }
        .footer { text-align: center; padding: 20px 0; border-top: 1px solid #eee; font-size: 12px; color: #999; }
      </style>
    </head>
    <body>
      <div class="container">
        <div class="header">
          <div class="logo">Easy1Auth</div>
        </div>
        <div class="content">
          <p>您好，</p>
          <p>您正在使用邮箱验证码登录 Easy1Auth，验证码如下：</p>
          <div class="code-box">
            <div class="code">${code}</div>
          </div>
          <p class="notice">验证码有效期为 5 分钟，请尽快使用。如非本人操作，请忽略此邮件。</p>
        </div>
        <div class="footer">
          <p>© ${new Date().getFullYear()} Easy1Auth. All rights reserved.</p>
        </div>
      </div>
    </body>
    </html>
  `

  return sendMail({
    to: email,
    subject: 'Easy1Auth 邮箱验证码',
    html,
    text: `您的验证码是: ${code}，有效期5分钟。`
  })
}
