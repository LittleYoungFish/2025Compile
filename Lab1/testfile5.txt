// void 函数 + break+continue
//  int getint() {
//      int t;
//      scanf("%d", &t);
//      while(getchar() != '\n');
//      return t;
//  }

// void函数：break+continue测试
void loop_test(int max)
{
    int res = 0;
    int i;
    for (i = 1; i <= max; i = i + 1)
    {
        if (i % 2 == 0)
            continue; // 跳过偶数
        if (i > 5)
            break; // 大于5终止
        res = res + i;
    }
    printf("loop_res=%d\n", res); // 嵌入printf（算写语句）
}

int main()
{
    printf("22241098\n"); // 1. 输出学号
    int m = getint();     // 读入input5.txt的8
    const int k = 3;

    printf("m*k=%d\n", m * k); // 2. 乘法
    loop_test(m);              // 3. 调用void函数（输出loop_res）

    int x = m - k;
    printf("m-k=%d\n", x); // 4. 减法

    // 空表达式语句
    ;                          // 合法空语句
    printf("empty_stmt_ok\n"); // 5. 空语句后提示

    int y = x / k;
    printf("x/k=%d\n", y); // 6. 除法
    printf("%d\n", x % k); // 7. 取模

    // if无else
    if (y > 0)
    {
        printf("y_positive\n"); // 8. 条件成立提示
    }

    printf("%d\n", y); // 9. 相等性运算
    printf("%d\n", m); // 10. 关系运算
    return 0;
}