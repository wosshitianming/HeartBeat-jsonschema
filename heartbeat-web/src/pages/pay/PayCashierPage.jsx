import {useState} from 'react'

export default function PayCashierPage() {
  const [channel, setChannel] = useState('wechat')

  return (
      <div className="hb-page-card hb-cashier-card">
        <p className="eyebrow">支付系统</p>
        <h1>收银台</h1>
        <p>参考 HeartBeat 后台的居中卡片支付体验（演示数据）。</p>
        <div className="hb-cashier-amount">¥ 128.00</div>
        <div className="hb-cashier-qr">扫码支付占位</div>
        <div style={{ display: 'flex', gap: 8, justifyContent: 'center' }}>
          <label>
            <input
                type="radio"
                name="pay-channel"
                value="wechat"
                checked={channel === 'wechat'}
                onChange={() => setChannel('wechat')}
            />
            微信支付
          </label>
          <label>
            <input
                type="radio"
                name="pay-channel"
                value="alipay"
                checked={channel === 'alipay'}
                onChange={() => setChannel('alipay')}
            />
            支付宝
          </label>
        </div>
      </div>
  )
}
