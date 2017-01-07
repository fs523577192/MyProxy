# MyProxy
国外的许多网站，例如github、stackoverflow、express.js的官网等等，都使用了google提供的前端库（Javascript的CDN：ajax.googleapis.com和字体库fonts.googleapis.com）。
但是中国大陆对google的服务器进行了封锁，这导致在中国大陆访问这些国外网站时，会由于加载不了需要的JS文件和字体文件而造成页面加载时间非常长，甚至页面显示有问题。
为了方便国内使用前端库，360提供了一个CDN来同步google的前端库：https://cdn.baomitu.com。
本代理小程序可以将对googleapis.com的访问转为对baomitu.com的访问，从而加速访问国外网站时的页面加载。
