const crypto = require('crypto')

/**
 * 后端 Spring Boot 服务器地址
 * 小程序运行在手机上，不能使用 localhost，需填本机实际局域网 IP
 * 例如后端运行在 192.168.1.100:8080，就填 http://192.168.1.100:8080
 */
const BACKEND_BASE_URL = 'http://192.168.0.111:8080'

function createCommonToken(params) {
    const access_key = Buffer.from( params.access_key, "base64")

    const version = params.version
    let res = 'products' + '/' + params.productid
    const et = Math.ceil((Date.now() + 365 * 24 * 3600 * 1000) / 1000)   
    const method = 'sha1'

    const key = et + "\n" + method + "\n" + res + "\n" + version
    
    let sign = crypto.createHmac('sha1', access_key).update(key).digest().toString('base64')

    res = encodeURIComponent(res)
    sign = encodeURIComponent(sign)
    const token = `version=${version}&res=${res}&et=${et}&method=${method}&sign=${sign}`

    return token
}

module.exports = {
  BACKEND_BASE_URL,
  createCommonToken
};


