import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  output: 'standalone',

  experimental: {
    middlewareClientMaxBodySize: '50mb',

    serverActions: {
      bodySizeLimit: '50mb',
    },
  },

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