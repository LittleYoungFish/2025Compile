// int getint() {
//     int t;
//     scanf("%d", &t);
//     while(getchar() != '\n');
//     return t;
// }

// 无返回值函数
void print_msg(int x)
{
    printf("msg_x=%d\n", x); // 嵌入printf（算写语句）
    return;
}

int main()
{
    printf("22241098\n"); // 1. 输出学号
    int a;
    a = getint();    // 读入input4.txt的7
    const int b = 4; // 局部常量

    printf("a+b=%d\n", a + b); // 2. 加法
    printf("a-b=%d\n", a - b); // 3. 减法
    printf("a*b=%d\n", a * b); // 4. 乘法

    // if-else语句
    if (a > b)
    {
        print_msg(a); // 调用无返回值函数
    }
    else
    {
        print_msg(b);
    } // 5. 嵌入printf的输出

    int c = a / b;
    printf("a/b=%d\n", c); // 6. 除法
    printf("%d\n", a % b); // 7. 取模

    // for循环（无缺省）
    int sum = 0, i;
    for (i = 1; i <= c; i = i + 1)
    {
        sum = sum + i;
    }
    printf("sum=1 to %d is %d\n", c, sum); // 8. 循环求和

    printf("%d\n", sum); // 9. 关系运算
    printf("a=%d\n", a); // 10. 相等性运算
    return 0;
}