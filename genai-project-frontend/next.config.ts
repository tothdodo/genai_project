import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  output: 'standalone',
  /* config options here */
  async rewrites() {
    return [
      {
        source: '/minio/:path*',
        destination: 'http://minio:9000/:path*'
      }
    ];
  }
};

export default nextConfig;
