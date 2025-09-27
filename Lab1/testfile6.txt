// 全局变量 + return + 简单函数
// int getint()
// {
//     int t;
//     scanf("%d", &t);
//     while (getchar() != '\n')
//         ;
//     return t;
// }

int g_global = 10; // 全局变量

int main()
{
    printf("22241098\n"); // 1. 输出学号
    int num = getint();   // 读入input6.txt的5

    printf("g_global_init=%d\n", g_global); // 2. 全局变量初始值
    int new_val = 15;
    printf("g_global_new=%d\n", new_val); // 3. 全局变量更新后

    printf("num+new_val=%d\n", num + new_val); // 4. 加法
    printf("new_val-num=%d\n", new_val - num); // 5. 减法

    {
        ;
    }

    // for循环（初始化多变量）
    int sum = 0, cnt = 1;
    for (; cnt <= 4; cnt = cnt + 1)
    {
        sum = sum + cnt;
    }
    printf("sum_1-4=%d\n", sum); // 6. 循环求和

    printf("%d\n", sum);     // 7. 关系运算
    printf("cnt=%d\n", cnt); // 8. 循环变量值

    // 调用无参数函数
    printf("func_no_param=%d\n", 3); // 9. 无参函数返回值

    printf("g_global+func_no_param=%d\n", g_global); // 10. 全局变量与函数运算
    return 0;
}