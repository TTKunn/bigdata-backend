@echo off
chcp 65001 >nul
echo ========================================
echo 购物车双重存储功能测试
echo ========================================
echo.

echo [测试1] 添加商品到购物车
curl -X POST "http://localhost:8080/api/cart/add?productId=000100000001&quantity=2"
echo.
echo.

echo [测试2] 查询购物车（从Redis读取）
curl -X GET "http://localhost:8080/api/cart"
echo.
echo.

echo [测试3] 清空Redis缓存（模拟缓存失效）
echo 请手动在Redis中执行: DEL cart:000000000001
pause
echo.

echo [测试4] 再次查询购物车（从HBase加载并回填Redis）
curl -X GET "http://localhost:8080/api/cart"
echo.
echo.

echo [测试5] 更新商品数量
curl -X PUT "http://localhost:8080/api/cart/update?productId=000100000001&quantity=5"
echo.
echo.

echo [测试6] 查询购物车（验证更新）
curl -X GET "http://localhost:8080/api/cart"
echo.
echo.

echo [测试7] 删除商品
curl -X DELETE "http://localhost:8080/api/cart/remove" -H "Content-Type: application/json" -d "[\"000100000001\"]"
echo.
echo.

echo [测试8] 查询购物车（验证删除）
curl -X GET "http://localhost:8080/api/cart"
echo.
echo.

echo ========================================
echo 测试完成！
echo ========================================
pause
