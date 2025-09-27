// int getint() {
//     int t;
//     scanf("%d", &t);
//     while(getchar() != '\n');
//     return t;
// }

// 全局数组（全初始化）
const int G_ARR[4] = {10, 20, 30, 40};

// 函数：多参数+数组元素修改
void modify_arr(int arr[], int idx, int val)
{
    arr[idx] = val;
}

int main()
{
    printf("22241098\n"); // 1. 输出学号
    int x, y;
    x = getint();   // 读入input2.txt的3
    y = getint();   // 读入input2.txt的5
    int loc_arr[5]; // 局部数组（无初始化）

    printf("G_ARR[1]=%d\n", G_ARR[1]);     // 2. 全局常量数组访问
    modify_arr(loc_arr, 0, x + y);         // 初始化局部数组第0个元素
    printf("loc_arr[0]=%d\n", loc_arr[0]); // 3. 局部数组访问

    // 数组循环赋值
    int i;
    for (i = 1; i < 5; i = i + 1)
    {
        loc_arr[i] = loc_arr[i - 1] + 2;
    }
    printf("loc_arr[3]=%d\n", loc_arr[3]); // 4. 循环赋值后数组元素

    printf("x-y=%d\n", x - y);               // 5. 减法运算
    printf("y/x=%d\n", y / x);               // 6. 除法运算
    printf("G_ARR[0]+x=%d\n", G_ARR[0] + x); // 7. 常量数组与变量运算

    // 函数多参数调用
    modify_arr(loc_arr, 2, G_ARR[2]);
    printf("modified_loc_arr[2]=%d\n", loc_arr[2]); // 8. 修改后数组元素

    printf("loc_arr[4]=%d\n", loc_arr[4]); // 9. 数组最后一个元素
    printf("%d\n", x);                     // 10. 关系运算
    return 0;
}